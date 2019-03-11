package com.example.basics;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

class StartStopActor1 extends AbstractActor {
    static Props props() {
        return Props.create(StartStopActor1.class, StartStopActor1::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchEquals("stop", s -> {
            getContext().stop(getSelf());
        }).build();
    }

    @Override
    public void preStart() {
        System.out.println("first started");
        getContext().actorOf(StartStopActor2.props(), "second");
    }

    @Override
    public void postStop() {
        System.out.println("first stopped");
    }
}

class StartStopActor2 extends AbstractActor {
    static Props props() {
        return Props.create(StartStopActor2.class, StartStopActor2::new);
    }


    @Override
    public Receive createReceive() {

        /**
         * Actor.emptyBehavior is a useful placeholder when we don't
         * want to handle any messages in the actor
         */
        return receiveBuilder().build();
    }

    @Override
    public void preStart() {
        System.out.println("second started");
    }

    @Override
    public void postStop() {
        System.out.println("second stopped");
    }
}

public class ActorPrePostBehaviors {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("testSystem");

        ActorRef first = system.actorOf(StartStopActor1.props(), "first");
        first.tell("stop", ActorRef.noSender());
    }

}
