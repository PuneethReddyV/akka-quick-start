package akka.quick.start.devices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.request.RequestDeviceGroupRepresentator;
import akka.quick.start.messages.response.RespondDeviceGroupRepresentator;
import akka.quick.start.users.UserDashBoardManager;

/**
 * This actor is responsible for all the entry and exit of group and acts as a bridge between {@link DeviceGroup device-group} and {@link UserDashBoardManager user-dash-board-manager}.
 * @author puneethvreddy
 *
 */
public class DeviceManager extends UntypedActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public static Props props() {
		return Props.create(DeviceManager.class);
	}

	final Map<String, ActorRef> groupIdToActor = new HashMap<>();
	final Map<ActorRef, String> actorToGroupId = new HashMap<>();

	@Override
	public void preStart() {
		log.info("DeviceManager started");
	}

	@Override
	public void postStop() {
		log.info("DeviceManager stopped");
	}

	private void onTrackDevice(RegisterGroupOrDevice trackMsg) {
		String groupId = trackMsg.getGroupId();
		ActorRef ref = groupIdToActor.get(groupId);
		if (ref != null) {
			ref.forward(trackMsg, getContext());
		} else {
			log.info("Creating device group actor for {}", groupId);
			ActorRef groupActor = getContext().actorOf(DeviceGroup.props(groupId), "group-" + groupId);
			getContext().watch(groupActor);
			groupIdToActor.put(groupId, groupActor);
			actorToGroupId.put(groupActor, groupId);
			groupActor.forward(trackMsg, getContext());
		}
	}

	private void onTerminated(Terminated t) {
		ActorRef groupActor = t.getActor();
		String groupId = actorToGroupId.get(groupActor);
		log.info("Device group actor for {} has been terminated", groupId);
		actorToGroupId.remove(groupActor);
		groupIdToActor.remove(groupId);
	}

	private void onRequestForGroupActors(RequestDeviceGroupRepresentator request){
		Set<ActorRef> returnValue = null;
		if(request.getGroupName() != null && !request.getGroupName().isEmpty()){
			if(request.getGroupName().size() == 1 && request.getGroupName().get(0).equals("*")){
				returnValue = this.actorToGroupId.keySet();
			}else{
				returnValue = request.getGroupName().stream()
				.filter(groupId -> groupIdToActor.get(groupId) != null)
				.map(ele -> groupIdToActor.get(ele))
				.collect(Collectors.toSet());
			
			}
		}
		getSender().tell(new RespondDeviceGroupRepresentator(request.getRequestId(), returnValue, request.getRequesterName(), request.getGroupName()), getSelf());

	}

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterGroupOrDevice){
			this.onTrackDevice((RegisterGroupOrDevice) message);
		}else if(message instanceof Terminated){
			this.onTerminated((Terminated) message);
		}else if(message instanceof RequestDeviceGroupRepresentator){
			this.onRequestForGroupActors((RequestDeviceGroupRepresentator) message);
		}else{
			log.error("Received unkown message-{} from {} to {}",message.toString(), getSender(), getSelf().path());
			unhandled(message);
		}
	}

}