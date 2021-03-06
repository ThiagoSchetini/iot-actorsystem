package com.example.iotsystem;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    public static final class RequestDeviceList {
        final long requestId;
        public RequestDeviceList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyDeviceList {
        final long requestId;
        final Set<String> ids;

        public ReplyDeviceList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    public static final class RequestMyId {
        final long requestId;
        public RequestMyId(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyMyId {
        final long requestId;
        final String myId;
        public ReplyMyId(long requestId, String myId) {
            this.requestId = requestId;
            this.myId = myId;
        }
    }

    public static final class RequestAllTemperatures {
        final long requestId;

        public RequestAllTemperatures(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyAllTemperatures {
        final long requestId;
        final Map<String, TemperatureReading> temperatures;
        public ReplyAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
            this.requestId = requestId;
            this.temperatures = temperatures;
        }
    }

    public interface TemperatureReading {}

    public static final class Temperature implements TemperatureReading {
        public final double value;

        public Temperature(double value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            return Double.compare(((Temperature) o).value, value) == 0;
        }

        @Override
        public int hashCode() {
            long l = Double.doubleToLongBits(value);
            return (int) (l ^ (l >>> 32));
        }

        @Override
        public String toString() {
            return "Temperature{value=" + value + "}";
        }
    }

    public enum TemperatureNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceTimedOut implements TemperatureReading {
        INSTANCE
    }

    @Override
    public void preStart() {
        log.info("Device group {} started", groupId);
    }

    @Override
    public void postStop() {
        log.info("Device group {} stopped", groupId);
    }

    private void onAllTemperatures(RequestAllTemperatures r) {

        //
        // Java collections are mutable, so we create a new one
        // since it's not safe many threads (actors) modifying the same structure
        //
        getContext().actorOf(DeviceGroupQuery.props(
                        new HashMap<>(this.actorToDeviceId), // -> the new one
                        r.requestId,
                        getSender(),
                        new FiniteDuration(3, TimeUnit.SECONDS)));
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
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(RequestMyId.class, this::onRequestId)
                .match(RequestAllTemperatures.class, this::onAllTemperatures)
                .match(Terminated.class, this::onTerminated)
                .build();
    }

}
