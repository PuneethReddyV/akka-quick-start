package akka.quick.start.devices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.devices.events.CollectionTimeout;
import akka.quick.start.devices.events.DeviceNotAvailable;
import akka.quick.start.devices.events.DeviceTimedOut;
import akka.quick.start.devices.events.Temperature;
import akka.quick.start.devices.events.TemperatureNotAvailable;
import akka.quick.start.devices.events.TemperatureReading;
import akka.quick.start.messages.request.RequestTemperature;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.RespondTemperature;
import scala.concurrent.duration.FiniteDuration;


/**
 * This actor is responsible for getting all the up-to-date temperatures from all the active devices.
 *  
 * @author puneethvreddy
 *
 */
public class DeviceGroupQuery extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	final Map<ActorRef, String> actorToDeviceId;
	final long requestId;
	final ActorRef requester;
	final Set<ActorRef> actorsNeedToRespond;
	final Map<String, TemperatureReading> returnValue = new HashMap<>();
	Cancellable queryTimeoutTimer;

	public DeviceGroupQuery(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout) {
		this.actorToDeviceId = actorToDeviceId;
		this.requestId = requestId;
		this.requester = requester;
		this.actorsNeedToRespond = this.actorToDeviceId.keySet();
		queryTimeoutTimer = getContext().system().scheduler().scheduleOnce(
				timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf()
				);
	}

	public static Props props(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout) {
		return Props.create(DeviceGroupQuery.class, actorToDeviceId, requestId, requester, timeout);
	}

	@Override
	public void preStart() {

		for (ActorRef deviceActor : this.actorToDeviceId.keySet()) {
			getContext().watch(deviceActor);
			deviceActor.tell(new RequestTemperature(this.requestId), getSelf());
		}
	}

	@Override
	public void postStop() {
		this.queryTimeoutTimer.cancel();
	}

	public void receivedResponse(ActorRef deviceActor,
			TemperatureReading reading,
			Set<ActorRef> stillWaiting,
			Map<String, TemperatureReading> repliesSoFar) {
		getContext().unwatch(deviceActor);
		String deviceId = this.actorToDeviceId.get(deviceActor);
		stillWaiting.remove(deviceActor);
		repliesSoFar.put(deviceId, reading);
		if (stillWaiting.isEmpty()) {
			this.requester.tell(new RespondAllTemperaturesOfDevicesInGroup(requestId, repliesSoFar), getSelf());
			getContext().stop(getSelf());
		} 
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RespondTemperature){
			ActorRef deviceActor = getSender();
			RespondTemperature r = (RespondTemperature) message;
			TemperatureReading reading  = r
					.getValue()
					.map(ele -> (TemperatureReading)new Temperature(ele))
					.orElse(new TemperatureNotAvailable());
			if(this.requestId == r.getRequestId()){
				receivedResponse(deviceActor, reading, this.actorsNeedToRespond, this.returnValue);
			}else{
				log.error("received {} request Id, but this actor is responsible only for {} request Id.", r.getRequestId(), this.requestId);
			}

		}else if(message instanceof Terminated){
			receivedResponse(((Terminated) message).getActor(), new DeviceNotAvailable(), this.actorsNeedToRespond, this.returnValue);
		}else if(message instanceof CollectionTimeout){

			Map<String, TemperatureReading> replies = new HashMap<>(returnValue);
			for (ActorRef deviceActor : actorsNeedToRespond) {
				String deviceId = actorToDeviceId.get(deviceActor);
				replies.put(deviceId, new DeviceTimedOut());
			}
			requester.tell(new RespondAllTemperaturesOfDevicesInGroup(requestId, replies), getSelf());
			getContext().stop(getSelf());
		}else{
			log.error("Received unkown message-{} from {} to {}",message.toString(), getSender(), getSelf().path());
			unhandled(message);
		}

	}


}
