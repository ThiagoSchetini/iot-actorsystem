package com.example.basics;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.example.basics.Greeter.Greet;
import com.example.basics.Greeter.WhoToGreet;
import com.example.basics.Printer.Greeting;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;


public class AkkaQuickstartTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testGreeterActorSendingOfGreeting() {
        final TestKit testKit = new TestKit(system);

                                                                // a placebo of printer actor
        final ActorRef greeter = system.actorOf(Greeter.props(  testKit.getRef()              ));

        // normal use of Greeter actor
        greeter.tell(new WhoToGreet("Tester"), ActorRef.noSender());
        greeter.tell(new Greet(), ActorRef.noSender());

        // takes the message that Greeter produced (the message contains Greeting)
        Greeting greeting = testKit.expectMsgClass(Greeting.class);

        // assert that something inside Greeting was correctly produced by Greeter
        assertEquals("Who to greet? -> Tester", greeting.message);
    }
}
