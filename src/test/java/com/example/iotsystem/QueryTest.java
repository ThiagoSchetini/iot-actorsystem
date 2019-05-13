package com.example.iotsystem;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import org.junit.*;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class QueryTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("system");
    }

    @Test
    public void returnTemperatureValuesForWorkingDevices() {

        //
        // this objects acts like a placebo
        // the requester is like a dummy to receive the replies that we need to test
        //
        TestKit requester = new TestKit(system);
        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        //
        // on preStart method we defined that the ReadTemperature will be sent
        // device 1 and 2 actors are acting like placebo
        //
        ActorRef queryActor = system.actorOf(
                DeviceGroupQuery.props(
                        actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(1, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).requestId);

        //
        // simulating RespondTemperature replies from placebos (device 1 and 2)
        //
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1d)), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2d)), device2.getRef());

        DeviceGroup.ReplyAllTemperatures reply = requester.expectMsgClass(DeviceGroup.ReplyAllTemperatures.class);
        assertEquals(1L, reply.requestId);

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1d));
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2d));
        assertEquals(expectedTemperatures, reply.temperatures);
    }

    @Test
    public void returnTemperatureNotAvailableForDevicesNoReading() {

        //
        // remember, they are like placebo actors
        //
        TestKit requester = new TestKit(system);
        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(
                DeviceGroupQuery.props(
                        actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(1, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.empty()), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2d)), device2.getRef());

        DeviceGroup.ReplyAllTemperatures reply = requester.expectMsgClass(DeviceGroup.ReplyAllTemperatures.class);
        assertEquals(1L, reply.requestId);

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", DeviceGroup.TemperatureNotAvailable.INSTANCE);
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2d));
        assertEquals(expectedTemperatures, reply.temperatures);
    }

    @Test
    public void returnDeviceNotAvailableForDeviceStopsBeforeAnswering() {
        TestKit requester = new TestKit(system);
        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(
                DeviceGroupQuery.props(
                        actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(1, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1d)), device1.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        DeviceGroup.ReplyAllTemperatures reply = requester.expectMsgClass(DeviceGroup.ReplyAllTemperatures.class);
        assertEquals(1L, reply.requestId);

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1d));
        expectedTemperatures.put("device2", DeviceGroup.DeviceNotAvailable.INSTANCE);
        assertEquals(expectedTemperatures, reply.temperatures);
    }

    @Test
    public void returnDeviceTemperatureEvenAfterRespondingItStops() {
        TestKit requester = new TestKit(system);
        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(
                DeviceGroupQuery.props(
                        actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(1, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1d)), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2d)), device2.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        DeviceGroup.ReplyAllTemperatures reply = requester.expectMsgClass(DeviceGroup.ReplyAllTemperatures.class);
        assertEquals(1L, reply.requestId);

        Map<String, DeviceGroup.Temperature> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1d));
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2d));
        assertEquals(expectedTemperatures, reply.temperatures);
    }

    @Test
    public void returnDeviceTimeoutIfDeviceNotReturnInTime() {
        TestKit requester = new TestKit(system);
        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> deviceToActorId = new HashMap<>();
        deviceToActorId.put(device1.getRef(), "device1");
        deviceToActorId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(
                DeviceGroupQuery.props(
                        deviceToActorId, 1L, requester.getRef(), new FiniteDuration(1, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1d)), device1.getRef());

        DeviceGroup.ReplyAllTemperatures reply = requester.expectMsgClass(
                Duration.ofSeconds(5), DeviceGroup.ReplyAllTemperatures.class);
        assertEquals(1L, reply.requestId);

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1d));
        expectedTemperatures.put("device2", DeviceGroup.DeviceTimedOut.INSTANCE);
        assertEquals(expectedTemperatures, reply.temperatures);
    }


}
