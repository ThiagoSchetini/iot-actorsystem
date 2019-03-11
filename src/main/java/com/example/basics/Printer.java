package com.example.basics;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;


public class Printer extends AbstractActor {

    private LoggingAdapter log;

    public Printer() {
        this.log = Logging.getLogger(getContext().getSystem(), this);
    }

    /**
     * why props?
     * It is a common pattern to use a static props method in the class of the Actor that describes how to construct the Actor
     *
     * Props is a configuration class to specify options for the creation of actors
     */
    static public Props props() {
        return Props.create(Printer.class, () -> new Printer());
    }

    /**
     * why nested class?
     * it is a good practice to put an actor’s associated messages as static classes in the class of the Actor.
     * This makes it easier to understand what type of messages the actor expects and handles
     *
     * why static?
     * only an enclosing inner class can be instantiated outside
     */
    static public class Greeting {

        // Messages should be immutable (final), since they are shared between different threads
        public final String message;
        public Greeting(String message) {
          this.message = message;
        }
    }

    /**
     * used by AKKA when .tell() method is invoked
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Greeting.class, greeting -> {
                log.info(greeting.message);
            })
            .build();
    }

}