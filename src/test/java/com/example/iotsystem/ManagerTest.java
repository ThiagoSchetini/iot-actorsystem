package com.example.iotsystem;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ManagerTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("system");
    }

    @AfterClass
    public static void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void registerDeviceGroupActor() {
        TestKit probe = new TestKit(system);
        ActorRef act = system.actorOf(DeviceManager.props());

        act.tell(new DeviceManager.RequestTrackDevice("group1", "deviceX"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceX = probe.getLastSender();

        act.tell(new DeviceManager.RequestTrackDevice("group2", "deviceY"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceY = probe.getLastSender();

        assertNotEquals(deviceX, deviceY);

        act.tell(new DeviceManager.RequestGroupList(0L), probe.getRef());
        DeviceManager.ReplyGroupList reply = probe.expectMsgClass(DeviceManager.ReplyGroupList.class);
        assertEquals(reply.requestId, 0L);
        assertEquals(Stream.of("group1", "group2").collect(Collectors.toSet()), reply.ids);
    }

    @Test
    public void receiveSameDeviceIdForDifferentGroup() {
        TestKit probe = new TestKit(system);
        ActorRef act = system.actorOf(DeviceManager.props());

        act.tell(new DeviceManager.RequestTrackDevice("group1", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceX = probe.getLastSender();

        act.tell(new DeviceManager.RequestTrackDevice("group2", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceY = probe.getLastSender();

        assertNotEquals(deviceX, deviceY);
    }


    @Test
    public void listGroups() {
        TestKit probe = new TestKit(system);
        ActorRef act = system.actorOf(DeviceManager.props());

        act.tell(new DeviceManager.RequestTrackDevice("group1", "device"), probe.getRef());
        act.tell(new DeviceManager.RequestTrackDevice("group2", "device"), probe.getRef());
        act.tell(new DeviceManager.RequestTrackDevice("group3", "device"), probe.getRef());
        act.tell(new DeviceManager.RequestGroupList(0L), probe.getRef());
        DeviceManager.ReplyGroupList reply = probe.expectMsgClass(DeviceManager.ReplyGroupList.class);
        assertEquals(reply.requestId, 0L);
        assertEquals(Stream.of("group1", "group2", "group3").collect(Collectors.toSet()), reply.ids);
    }

    @Test
    public void listGroupsAfterOneShutdown() {
        TestKit probe = new TestKit(system);
        ActorRef act = system.actorOf(DeviceManager.props());

        act.tell(new DeviceManager.RequestTrackDevice("group1", "device"), probe.getRef());
        act.tell(new DeviceManager.RequestTrackDevice("group2", "device"), probe.getRef());
        act.tell(new DeviceManager.RequestGroupList(0L), probe.getRef());
        DeviceManager.ReplyGroupList reply = probe.expectMsgClass(DeviceManager.ReplyGroupList.class);
        assertEquals(reply.requestId, 0L);



    }



}
