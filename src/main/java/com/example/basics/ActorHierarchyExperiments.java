package com.example.basics;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * AKKA actor system has three initial actors: root (/), system(/yoursystem) and user (/user)
 * we are showing below some paths to demonstrate it
 */
class PrintActorReference extends AbstractActor {

    static Props props() {
        return Props.create(PrintActorReference.class, PrintActorReference::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchEquals("printit", p -> {

            /**
             * the second actor path: testSystem/user/first-actor/second-actor
             * because it's been created inside the first-actor context
             */
            ActorRef secondRef = getContext().actorOf(Props.empty(), "second-actor");
            System.out.println("Second: " + secondRef);


            /**
             * the another actor path: testSystem/user/another-actor
             * because it's been created on the main (user) context
             */
            ActorRef anotherRef = getContext().getSystem().actorOf(Props.empty(), "another-actor");
            System.out.println("Another: " + anotherRef);
        }).build();
    }
}

public class ActorHierarchyExperiments {

    public static void main(String[] args) {
        ActorSystem testSystem = ActorSystem.create("testSystem");

        /**
         * the first actor path: testSystem/user/first-actor
         * because it's been created on the main (user) context
         */
        ActorRef firstRef = testSystem.actorOf(PrintActorReference.props(), "first-actor");
        System.out.println("First: " + firstRef);


        firstRef.tell("printit", ActorRef.noSender());
        System.out.println(">>> Press ENTER to exit <<<");

        try {
            System.in.read();
        } catch (Exception e) {
            System.out.println(e.getCause());
        } finally {
            testSystem.terminate();
        }
    }
}