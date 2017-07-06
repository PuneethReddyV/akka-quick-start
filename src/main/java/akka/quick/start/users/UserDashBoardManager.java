package akka.quick.start.users;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.ProcessingError;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.request.RequestActiveUserList;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.request.RequestDeviceGroupRepresentator;
import akka.quick.start.messages.response.RespondActiveUserList;
import akka.quick.start.messages.response.RespondDeviceGroupRepresentator;


/**
 * This actor manages all the users and acts as a bridge between the actors and {@link DeviceManager device-manager}
 * @author puneethvreddy
 *
 */

public class UserDashBoardManager extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private ActorRef deviceSideManager;

	public UserDashBoardManager(ActorRef deviceSideManager){
		this.deviceSideManager = deviceSideManager;
	}

	public static Props props(ActorRef deviceSideManager) {
		return Props.create(UserDashBoardManager.class, deviceSideManager);
	}

	final Map<String, ActorRef> userIdToActor = new HashMap<>();
	final Map<ActorRef, String> actorToUserId = new HashMap<>();

	@Override
	public void preStart() {
		log.info("UserDashBoardManager started");
	}

	@Override
	public void postStop() {
		log.info("UserDashBoardManager stopped");
	}
	
	private void onRegisterUser(RegisterUser userDetails){
		if(!userIdToActor.containsKey(userDetails.getUserName())){
			ActorRef userDashBoard = getContext().actorOf(UsersDashboard.props(userDetails.getUserName()));
			getContext().watch(userDashBoard);
			userIdToActor.put(userDetails.getUserName(), userDashBoard);
			actorToUserId.put(userDashBoard, userDetails.getUserName());
		}
		userIdToActor.get(userDetails.getUserName()).forward(userDetails, getContext());
	}
	
	private void onRequestForAllDeviceGroupsByUser(RequestAllTemperaturesOfDevicesInGroup request){
		if(userIdToActor.get(request.getRequesterName()) != null){
			this.deviceSideManager.tell(new RequestDeviceGroupRepresentator(request.getRequestId(), request.getRequesterName(), request.getGroupNames()), getSelf());
		}else{
			getSender().tell(
					new ProcessingError(
							String.format(
									"Hello %s, We have error message=\"%s\", before requesting for \"%s\" group details.", 
									request.getRequesterName(), 
									"Please register your self!!!", 
									request.getGroupNames()
									)
							),
					getSelf()
					);
		}
	}
	
	private void onResponseToDeviceGroupsByUser(RespondDeviceGroupRepresentator response){
		if(userIdToActor.get(response.getRequesterName()) != null){
			if(response.getListOfGroupActors() == null || response.getListOfGroupActors().isEmpty()){
				getSender().tell(
						new ProcessingError(
								String.format(
										"Dear %s, we no have any devices registered to this %s", 
										response.getRequesterName(), 
										response.getGroupName()
										)
								), 
						getSelf()
						);
			}else{
				response.getListOfGroupActors().stream().
				forEach(groupRepresentative -> groupRepresentative.tell(
						new RequestAllTemperaturesOfDevicesInGroup(response.getRequestId(), response.getGroupName(), response.getRequesterName()), 
						userIdToActor.get(response.getRequesterName())
						)
						);
			}
		}else{
			getSender().tell(new ProcessingError("I have ended up no where!!!"), getSelf());
		}
	}
	
	private void onTemination(Terminated actor ){
		ActorRef actorRef = actor.getActor();
		String ref = actorToUserId.get(actorRef);
		actorToUserId.remove(actorRef);
		userIdToActor.remove(ref);
		log.error("Actor -{}- have been killed at path -{}- ",actorRef, actorRef.path());
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterUser){
			this.onRegisterUser((RegisterUser) message);
		}else if(message instanceof RequestAllTemperaturesOfDevicesInGroup){
			this.onRequestForAllDeviceGroupsByUser((RequestAllTemperaturesOfDevicesInGroup)message);
		}else if(message instanceof RespondDeviceGroupRepresentator){
			this.onResponseToDeviceGroupsByUser((RespondDeviceGroupRepresentator) message);
		}else if(message instanceof Terminated){
			this.onTemination((Terminated) message);
		}else if(message instanceof RequestActiveUserList){
			 getSender().tell(new RespondActiveUserList(((RequestActiveUserList) message).getResquestId(), this.actorToUserId.keySet()), getSelf());
		}else{
			log.error("Received unknown message -{}- from -{}- to -{}", message.toString(), getSender(), getSelf());
		}
	}


}
