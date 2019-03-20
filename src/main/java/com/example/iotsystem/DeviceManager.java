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

    public static final class RequestGroupIdList {
        final long requestId;
        public RequestGroupIdList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyGroupIdList {
        final long requestId;
        final Set<String> ids;

        public ReplyGroupIdList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    public static final class RequestGroupActorList {
        final long requestId;
        public RequestGroupActorList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyGroupActorList {
        final long requestId;
        final Set<ActorRef> actors;

        public ReplyGroupActorList(long requestId, Set<ActorRef> actors) {
            this.requestId = requestId;
            this.actors = actors;
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

    private void onGroupIdList(RequestGroupIdList r) {
        getSender().tell(new ReplyGroupIdList(r.requestId, this.groupIdToActor.keySet()), getSelf());
    }

    private void onGroupActorList(RequestGroupActorList r) {
        getSender().tell(new ReplyGroupActorList(r.requestId, this.actorToGroupId.keySet()), getSelf());
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
                .match(RequestGroupIdList.class, this::onGroupIdList)
                .match(RequestGroupActorList.class, this::onGroupActorList)
                .match(Terminated.class, this::onTerminated)
                .build();
    }
}