package com.example.iotsystem;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class DeviceGroupQuery extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    final long requestID;
    final ActorRef requester;
    final Map<ActorRef, String> actorToDeviceId;

    Cancellable queryTimeoutTimer;

    public DeviceGroupQuery(
            Map<ActorRef, String> actorToDeviceId,
            long requestID,
            ActorRef requester,
            FiniteDuration timeout) {

        this.requestID = requestID;
        this.requester = requester;
        this.actorToDeviceId = actorToDeviceId;

        queryTimeoutTimer = getContext().getSystem().scheduler().scheduleOnce(
                timeout,
                getSelf(),
                new CollectionTimeout(),
                getContext().getSystem().getDispatcher(),
                getSelf());
    }

    public static final class CollectionTimeout {}

    public static Props props(
            Map<ActorRef, String> actorToDeviceId,
            long requestID,
            ActorRef requester,
            FiniteDuration timeout) {

        return Props.create(
                DeviceGroupQuery.class,
                () -> new DeviceGroupQuery(actorToDeviceId, requestID, requester, timeout));
    }

    @Override
    public void preStart() {
        for(ActorRef deviceActor : actorToDeviceId.keySet()) {
            getContext().watch(deviceActor);
            deviceActor.tell(new Device.ReadTemperature(0L), getSelf());
        }
    }

    @Override
    public void postStop() {
        queryTimeoutTimer.cancel();
    }

    @Override
    public Receive createReceive() {
        return waitingForReplies(new HashMap<>(), actorToDeviceId.keySet());
    }

    /**
     * function that will be used by become() to hotswap the Receive obj on execution
     * the "become(receive)" is called always on preStart() and get the Receive ojb from createReceive()
     *
     * @param repliesSoFar "updated" by become
     * @param stillWaiting "updated" by become
     * @return a brand new Receive that will handle the message
     */
    private Receive waitingForReplies(HashMap<String, DeviceGroup.TemperatureReading> repliesSoFar,
                                      Set<ActorRef> stillWaiting) {

        return receiveBuilder()
                .match(Device.RespondTemperature.class, r -> {
                    //ActorRef deviceActor = getSender();

                    DeviceGroup.TemperatureReading reading = r.value
                            .map(v -> (DeviceGroup.TemperatureReading) new DeviceGroup.Temperature(v))
                            .orElse(DeviceGroup.TemperatureNotAvailable.INSTANCE);

                    //receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar);
                    receivedResponse(getSender(), reading, stillWaiting, repliesSoFar);

                }).match(Terminated.class, r -> {
                    receivedResponse(getSender(), DeviceGroup.DeviceNotAvailable.INSTANCE, stillWaiting, repliesSoFar);

                }).match(CollectionTimeout.class, r -> {
                    final HashMap<String, DeviceGroup.TemperatureReading> replies = new HashMap<>(repliesSoFar);
                    actorToDeviceId.forEach((k,v) -> replies.put(actorToDeviceId.get(k), DeviceGroup.DeviceTimeout.INSTANCE));

                    requester.tell(new DeviceGroup.ReplyAllTemperatures(requestID, replies), getSelf());
                    getContext().stop(getSelf());

                }).build();
    }

    private void receivedResponse(ActorRef deviceActor,
                                  DeviceGroup.TemperatureReading reading,
                                  Set<ActorRef> stillWaiting,
                                  HashMap<String, DeviceGroup.TemperatureReading> repliesSoFar) {

        getContext().unwatch(deviceActor);
        String deviceId = actorToDeviceId.get(deviceActor);

        HashSet<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
        newStillWaiting.remove(deviceActor);

        HashMap<String, DeviceGroup.TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
        newRepliesSoFar.put(deviceId, reading);

        if(newStillWaiting.isEmpty()) {
            requester.tell(new DeviceGroup.ReplyAllTemperatures(requestID, newRepliesSoFar), getSelf());
            getContext().stop(getSelf());
        } else {

            /**
             * Hot swapping: the returned Receive object is changed
             * on the next device reply message, the new replies and waiting structures will be used
             */
            getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
        }
    }


}
