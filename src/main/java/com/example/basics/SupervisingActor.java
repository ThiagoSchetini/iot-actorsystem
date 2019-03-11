package com.example.basics;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.util.Optional;


class SupervisingActor extends AbstractActor {
    static Props props() {
        return Props.create(SupervisingActor.class, SupervisingActor::new);
    }

    ActorRef child = getContext().actorOf(SupervisedActor.props(), "supervised-actor");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "failChild",
                        f -> {
                            child.tell("fail", getSelf());
                        })
                .build();
    }
}


class SupervisedActor extends AbstractActor {
    static Props props() {
        return Props.create(SupervisedActor.class, SupervisedActor::new);
    }

    @Override
    public void preStart() {
        System.out.println("supervised actor pre started");
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        System.out.println("supervised actor is being pre restarted...");

        // super defaults to sending termination requests to all children and calling postStop()
        super.preRestart(reason, message);
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        System.out.println("supervised actor restarting");

        // super default also calls preStart()
        super.postRestart(reason);
    }

    @Override
    public void postStop() {
        System.out.println("supervised actor stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "fail",
                        f -> {
                            System.out.println("supervised actor fails now");
                            throw new Exception("I failed!");
                        })
                .build();
    }
}


class Execute {
    public static void main(String[] args) {

        ActorSystem system = ActorSystem.create("system");
        ActorRef supervisor = system.actorOf(SupervisingActor.props(), "supervisor-actor");
        supervisor.tell("failChild", ActorRef.noSender());

    }
}