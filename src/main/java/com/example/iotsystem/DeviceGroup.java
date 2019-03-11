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

    @Override
    public void preStart() {
        log.info("Device group {} started", groupId);
    }

    @Override
    public void postStop() {
        log.info("Device group {} stopped", groupId);
    }

    private void onTrackDevice(DeviceManager.RequestTrackDevice trackingMsg) {
        if (trackingMsg.groupId.equals(this.groupId)) {

            final ActorRef deviceActor = deviceIdToActor.get(trackingMsg.deviceId);

            if(deviceActor != null) {
                deviceActor.forward(trackingMsg, getContext());
            } else {
                createDeviceActor(trackingMsg).forward(trackingMsg, getContext());
            }

        } else {
            log.warning(
                    "Ignoring Track device for group {}. This actor is responsible for group {}.",
                    trackingMsg.groupId,
                    this.groupId
            );
        }
    }

    private void onTerminated(Terminated t) {
        ActorRef terminatedActor = t.getActor();
        String terminatedId = actorToDeviceId.get(terminatedActor);

        log.info("the device actor for {} has been terminated", terminatedId);

        actorToDeviceId.remove(terminatedActor);
        deviceIdToActor.remove(terminatedId);
    }

    private void onDeviceList(RequestDeviceList r) {
        getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), getSelf());
    }

    private ActorRef createDeviceActor(DeviceManager.RequestTrackDevice msg) {
        log.info("creating actor for device {}", msg.deviceId);

        ActorRef deviceActor = getContext().actorOf(
                Device.props(msg.groupId, msg.deviceId),
                "device-" + msg.deviceId);

        deviceIdToActor.put(msg.deviceId, deviceActor);
        actorToDeviceId.put(deviceActor, msg.deviceId);
        return deviceActor;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(Terminated.class, this::onTerminated)
                .build();
    }

}
