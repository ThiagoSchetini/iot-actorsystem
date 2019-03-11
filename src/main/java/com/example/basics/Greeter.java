package com.example.basics;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;


public class Greeter extends AbstractActor {

    private final ActorRef printerActor;
    private String greeting = "";

    public Greeter(ActorRef printerActor) {
        this.printerActor = printerActor;
    }

    /**
     * why props?
     * It is a common pattern to use a static props method in the class of the Actor that describes how to construct the Actor
     *
     * Props is a configuration class to specify options for the creation of actors
     */
    static public Props props(ActorRef printerActor) {
        return Props.create(Greeter.class, () -> new Greeter(printerActor));
    }

    /**
     * why nested class?
     * it is a good practice to put an actor’s associated messages as static classes in the class of the Actor.
     * This makes it easier to understand what type of messages the actor expects and handles
     *
     * why static?
     * only an enclosing inner class can be instantiated outside
     */
    static public class WhoToGreet {

        // Messages should be immutable (final), since they are shared between different threads
        public final String who;
        public WhoToGreet(String who) {
            this.who = who;
        }
    }

    /**
     * why nested class?
     * it is a good practice to put an actor’s associated messages as static classes in the class of the Actor.
     * This makes it easier to understand what type of messages the actor expects and handles
     *
     * why static?
     * only an enclosing inner class can be instantiated outside
     */
    static public class Greet {
        public Greet() {}
    }

    /**
     * used by AKKA when .tell() method is invoked
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()

            .match(WhoToGreet.class, wtg -> {
                // -> uses the internal state of wtg and modify this internal state (this.greeting)
                this.greeting = "Who to greet? -> " + wtg.who;

            }).match(Greet.class, x -> {
                // -> send a message to another actor (thread)
                printerActor.tell(new Printer.Greeting(greeting), getSelf());

            }).build();
    }

}

