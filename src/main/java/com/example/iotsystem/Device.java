package com.example.iotsystem;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Optional;

import com.example.iotsystem.DeviceManager.RequestTrackDevice;
import com.example.iotsystem.DeviceManager.DeviceRegistered;



public class Device extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    final String groupId;
    final String deviceId;

    Optional<Double> lastTemperatureReading = Optional.empty();

    public Device(String groupId, String deviceId) {
        this.deviceId = deviceId;
        this.groupId = groupId;
    }

    public static Props props(String groupId, String deviceId) {
        return Props.create(Device.class, () -> new Device(groupId, deviceId));
    }

    public static final class RecordTemperature {
        final long requestId;
        final double value;

        public RecordTemperature(long requestId, double value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    public static final class TemperatureRecorded {
        final long requestId;

        public TemperatureRecorded(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReadTemperature {
        final long requestId;

        public ReadTemperature(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class RespondTemperature {
        final long requestId;
        final Optional<Double> value;

        public RespondTemperature(long requestId, Optional<Double> value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    @Override
    public void preStart() {
        log.info("Device Actor {}-{} started", groupId, deviceId);
    }

    @Override
    public void postStop() {
        log.info("Device Actor {}-{} stopped", groupId, deviceId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                .match(RequestTrackDevice.class, r -> {
                    if (this.groupId.equals(r.groupId) && this.deviceId.equals(r.deviceId)) {
                        getSender().tell(new DeviceRegistered(), getSelf());
                    } else {
                        log.warning(
                            "Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
                            r.groupId,
                            r.deviceId,
                            this.groupId,
                            this.deviceId);
                    }
                })

                .match(ReadTemperature.class, r -> {
                    getSender().tell(new RespondTemperature(r.requestId, this.lastTemperatureReading), getSelf());
                })

                .match(RecordTemperature.class, r -> {
                    log.info("Recorded Temperature reading with {} and {}", r.value, r.requestId);
                    lastTemperatureReading = Optional.of(r.value);
                    getSender().tell(new TemperatureRecorded(r.requestId), getSelf());
                })

                .build();
    }
}