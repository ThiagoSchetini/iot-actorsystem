package com.example.iotsystem;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
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

        act.tell(new DeviceManager.RequestGroupIdList(0L), probe.getRef());
        DeviceManager.ReplyGroupIdList reply = probe.expectMsgClass(DeviceManager.ReplyGroupIdList.class);
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("group1", "group2").collect(Collectors.toSet()), reply.ids);
    }

    @Test
    public void listGroups() {
        TestKit probe = new TestKit(system);
        ActorRef act = system.actorOf(DeviceManager.props());

        act.tell(new DeviceManager.RequestTrackDevice("group1", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        act.tell(new DeviceManager.RequestTrackDevice("group2", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        act.tell(new DeviceManager.RequestTrackDevice("group3", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);

        act.tell(new DeviceManager.RequestGroupIdList(0L), probe.getRef());
        DeviceManager.ReplyGroupIdList reply = probe.expectMsgClass(DeviceManager.ReplyGroupIdList.class);
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("group1", "group2", "group3").collect(Collectors.toSet()), reply.ids);
    }

    @Test
    public void listGroupsAfterOneShutdown() {
        TestKit probe = new TestKit(system);
        ActorRef act = system.actorOf(DeviceManager.props());

        act.tell(new DeviceManager.RequestTrackDevice("group1", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        act.tell(new DeviceManager.RequestTrackDevice("group2", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);

        act.tell(new DeviceManager.RequestGroupActorList(0L), probe.getRef());
        DeviceManager.ReplyGroupActorList reply = probe.expectMsgClass(DeviceManager.ReplyGroupActorList.class);
        assertEquals(0L, reply.requestId);
        assertEquals(2, reply.actors.size());

        ActorRef toShutdown = reply.actors.stream().findAny().get();
        probe.watch(toShutdown);
        toShutdown.tell(PoisonPill.getInstance(), probe.getRef());
        probe.expectTerminated(toShutdown);

        probe.awaitAssert(() -> {
            act.tell(new DeviceManager.RequestGroupActorList(1L), probe.getRef());
            DeviceManager.ReplyGroupActorList reply2 = probe.expectMsgClass(DeviceManager.ReplyGroupActorList.class);
            assertEquals(1, reply2.actors.size());
            return null;
        });
    }


}
