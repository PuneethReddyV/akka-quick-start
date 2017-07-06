package akka.quick.start.devices;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.request.RequestActiveDeviceListOfGroup;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.RespondActiveDeviceListOfGroup;
import scala.concurrent.duration.FiniteDuration;


/**
 * This actor responsible for a new device entry and exit from a group.<br>
 * Provides up-to-date temperatures of all active devices using {@link DeviceGroupQuery cameo-actor}.
 * 
 * @author puneethvreddy
 *
 */
public class DeviceGroup extends UntypedActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	final String groupId;

	public DeviceGroup(String groupId) {
		this.groupId = groupId;
	}

	public static Props props(String groupId) {
		return Props.create(DeviceGroup.class, groupId);
	}

	
	final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
	final Map<ActorRef, String> actorToDeviceId = new HashMap<>();

	@Override
	public void preStart() {
		log.info("DeviceGroup {} started", groupId);
	}

	@Override
	public void postStop() {
		log.info("DeviceGroup {} stopped", groupId);
	}

	private void onTrackDevice(RegisterGroupOrDevice trackMsg) {
		System.out.println(getSender().path()+ " :: "+actorToDeviceId.size());
		if (this.groupId.equals(trackMsg.getGroupId())) {
			ActorRef deviceActor = deviceIdToActor.get(trackMsg.getDeviceId());
			if (deviceActor != null) {
				deviceActor.forward(trackMsg, getContext());
			} else {
				log.info("Creating device actor for {}", trackMsg.getDeviceId());
				deviceActor = getContext().actorOf(DeviceWorker.props(groupId, trackMsg.getDeviceId()), "device-" + trackMsg.getDeviceId());
				getContext().watch(deviceActor);
				actorToDeviceId.put(deviceActor, trackMsg.getDeviceId());
				deviceIdToActor.put(trackMsg.getDeviceId(), deviceActor);
				deviceActor.forward(trackMsg, getContext());
			}
		} else {
			log.warning(
					"Ignoring TrackDevice request for {}. This actor is responsible for {}.",
					groupId, this.groupId
					);
		}
		System.out.println(trackMsg+" :: "+getSender().path()+ " :: "+actorToDeviceId.size());

	}

	private void onDeviceList(RequestActiveDeviceListOfGroup r) {
		getSender().tell(new RespondActiveDeviceListOfGroup(r.requestId, deviceIdToActor.keySet()), getSelf());
	}

	private void onTerminated(Terminated t) {
		ActorRef deviceActor = t.getActor();
		String deviceId = actorToDeviceId.get(deviceActor);
		log.info("Device actor for {} has been terminated", deviceId);
		actorToDeviceId.remove(deviceActor);
		deviceIdToActor.remove(deviceId);
	}


	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterGroupOrDevice){
			RegisterGroupOrDevice requestTrackDevice = (RegisterGroupOrDevice) message;
			this.onTrackDevice(requestTrackDevice);
		}else if(message instanceof RequestActiveDeviceListOfGroup){
			RequestActiveDeviceListOfGroup requestDeviceList = (RequestActiveDeviceListOfGroup) message;
			this.onDeviceList(requestDeviceList);
		}else if( message instanceof Terminated){
			this.onTerminated((Terminated) message);
		}else if(message instanceof RequestAllTemperaturesOfDevicesInGroup){
			RequestAllTemperaturesOfDevicesInGroup r = (RequestAllTemperaturesOfDevicesInGroup) message;
			getContext().actorOf(DeviceGroupQuery.props(actorToDeviceId, r.getRequestId(), getSender(), new FiniteDuration(3, TimeUnit.SECONDS))); 
		}else {
			log.error("Received unkown message-{} from {} to {}",message.toString(), getSender(), getSelf().path());
			unhandled(message);
		}		
	}
}
