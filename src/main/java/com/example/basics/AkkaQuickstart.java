package com.example.basics;

import java.io.IOException;

import com.example.basics.Greeter.Greet;
import com.example.basics.Greeter.WhoToGreet;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class AkkaQuickstart {

    public static void main(String[] args) {

        final ActorSystem factory = ActorSystem.create("factory");

        try {
            /* create actors
             * greeters receive the same actor printer reference
             *
             * In Akka you can’t create an instance of an Actor using the new keyword.
             * Instead, you create Actor instances using a factory that returns a reference
             *
             * akka.actor.ActorSystem is a factory
             */
            final ActorRef printer = factory.actorOf(Printer.props(), "printer");
            final ActorRef greeter1 = factory.actorOf(Greeter.props(printer), "greeter1");
            final ActorRef greeter2 = factory.actorOf(Greeter.props(printer), "greeter2");
            final ActorRef greeter3 = factory.actorOf(Greeter.props(printer), "greeter3");


            /*
             * send messages: WhoToGreet and Greet
             * greeter actor: receives WhoToGreet and Greet messages
             *
             * .tell() creates a new thread:
             * second is printed after third and fourth because of Multithreading computation
             */
            greeter1.tell(new WhoToGreet("first"), ActorRef.noSender());       // update the greeter state
            greeter1.tell(new Greet(), ActorRef.noSender());                   // trigger a sending to the printer actor

            greeter1.tell(new WhoToGreet("second"), ActorRef.noSender());
            greeter1.tell(new Greet(), ActorRef.noSender());

            greeter2.tell(new WhoToGreet("third"), ActorRef.noSender());
            greeter2.tell(new Greet(), ActorRef.noSender());

            greeter3.tell(new WhoToGreet("fourth"), ActorRef.noSender());       // update the greeter3 state
            greeter3.tell(new WhoToGreet("fourth again"), ActorRef.noSender()); // update again the greeter3 state
            greeter3.tell(new Greet(), ActorRef.noSender());


            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

        } catch (IOException ioe) {
        } finally { factory.terminate(); } // shut down the actor factory
    }
}
