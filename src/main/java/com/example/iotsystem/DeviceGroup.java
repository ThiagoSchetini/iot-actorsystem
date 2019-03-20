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

public class DeviceGroup extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    final String groupId;
    final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
    final Map<ActorRef, String> actorToDeviceId = new HashMap<>();

    public DeviceGroup(String groupId) {
        this.groupId = groupId;
    }

    public static Props props(String groupId) {
        return Props.create(DeviceGroup.class, () -> new DeviceGroup(groupId));
    }

    public static class RequestDeviceList {
        final long requestId;
        public RequestDeviceList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static class ReplyDeviceList {
        final long requestId;
        final Set<String> ids;

        public ReplyDeviceList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    public static class RequestMyId {
        final long requestId;
        public RequestMyId(long requestId) {
            this.requestId = requestId;
        }
    }

    public static class ReplyMyId {
        final long requestId;
        final String myId;
        public ReplyMyId(long requestId, String myId) {
            this.requestId = requestId;
            this.myId = myId;
        }
    }

    private void onTrackDevice(DeviceManager.RequestTrackDevice msg) {
        if (msg.groupId.equals(this.groupId)) {
            ActorRef act = deviceIdToActor.get(msg.deviceId);

            if(act != null) {
                act.forward(msg, getContext());

            } else {
                log.info("creating actor for device {}", msg.deviceId);
                act = getContext().actorOf(Device.props(msg.groupId, msg.deviceId), "device-" + msg.deviceId);
                deviceIdToActor.put(msg.deviceId, act);
                actorToDeviceId.put(act, msg.deviceId);
                getContext().watch(act);
                act.forward(msg, getContext());
            }

        } else {
            log.warning(
                    "Ignoring Track device for group {}. This actor is responsible for group {}.",
                    msg.groupId,
                    this.groupId
            );
        }
    }

    private void onDeviceList(RequestDeviceList r) {
        getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), getSelf());
    }

    private void onRequestId(RequestMyId r) {
        getSender().tell(new ReplyMyId(r.requestId, this.groupId), getSelf());
    }

    private void onTerminated(Terminated t) {
        ActorRef act = t.getActor();
        String id = actorToDeviceId.get(act);
        actorToDeviceId.remove(act);
        deviceIdToActor.remove(id);
        log.info("the device actor for {} has been terminated", id);
    }

    @Override
    public void preStart() {
        log.info("Device group {} started", groupId);
    }

    @Override
    public void postStop() {
        log.info("Device group {} stopped", groupId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(RequestMyId.class, this::onRequestId)
                .match(Terminated.class, this::onTerminated)
                .build();
    }

}
