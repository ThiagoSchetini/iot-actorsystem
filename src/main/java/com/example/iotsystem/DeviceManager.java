package com.example.iotsystem;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeviceManager extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    final Map<String, ActorRef> groupIdToActor = new HashMap<>();
    final Map<ActorRef, String> actorToGroupId = new HashMap<>();

    public static Props props() {
        return Props.create(DeviceManager.class, DeviceManager::new);
    }

    public static final class RequestTrackDevice {

        public final String groupId;
        public final String deviceId;

        public RequestTrackDevice(String groupId, String deviceId) {
            this.groupId = groupId;
            this.deviceId = deviceId;
        }
    }

    public static final class DeviceRegistered {}

    public static final class RequestGroupList {
        final long requestId;
        public RequestGroupList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyGroupList {
        final long requestId;
        final Set<String> ids;

        public ReplyGroupList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    private void onTrackDevice(RequestTrackDevice msg) {
        ActorRef act = groupIdToActor.get(msg.groupId);

        if(act != null) {
            act.forward(msg, getContext());

        } else {
            act = getContext().actorOf(DeviceGroup.props(msg.groupId), "group-" + msg.groupId);
            groupIdToActor.put(msg.groupId, act);
            actorToGroupId.put(act, msg.groupId);
            getContext().watch(act);
            act.forward(msg, getContext());
        }
    }

    private void onGroupList(RequestGroupList r) {
        getSender().tell(new ReplyGroupList(r.requestId, this.groupIdToActor.keySet()), getSelf());
    }

    private void onTerminated(Terminated t) {
        ActorRef act = t.getActor();
        String id = actorToGroupId.get(act);
        log.info("Device group actor for {} has been terminated", id);
        actorToGroupId.remove(act);
        groupIdToActor.remove(id);
    }

    @Override
    public void preStart() {
        log.info("DeviceManager actor started");
    }

    @Override
    public void postStop() {
        log.info("DeviceManager stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestGroupList.class, this::onGroupList)
                .match(Terminated.class, this::onTerminated)
                .build();
    }
}