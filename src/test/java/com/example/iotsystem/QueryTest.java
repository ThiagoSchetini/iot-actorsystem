package com.example.iotsystem;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import org.junit.*;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class QueryTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("system");
    }

    @Test
    public void returnTemperatureValuesForWorkingDevices() {

        /**
         * this objects acts like a placebo
         */
        TestKit requester = new TestKit(system);
        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        HashMap<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        /**
         * on preStart method we defined that the ReadTemperature will be sent
         * device 1 and 2 actors are acting like placebo
         */
        ActorRef queryActor = system.actorOf(
                DeviceGroupQuery.props(
                        actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).requestId);

        /**
         * simulating RespondTemperature replies from placebos (device 1 and 2)
         */
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1d)), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2d)), device2.getRef());

        DeviceGroup.ReplyAllTemperatures reply = requester.expectMsgClass(DeviceGroup.ReplyAllTemperatures.class);
        assertEquals(1L, reply.requestId);

        HashMap<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1d));
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2d));
        assertEquals(expectedTemperatures, reply.temperatures);
    }

}
