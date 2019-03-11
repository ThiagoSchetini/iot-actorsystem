package com.example.iotsystem;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

public class IotMain {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("iot-system");

        ActorRef iotSupervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor");
        System.out.println("PRESS ENTER to exit the system");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }

    }

}
