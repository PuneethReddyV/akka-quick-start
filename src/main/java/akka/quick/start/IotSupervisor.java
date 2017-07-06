package akka.quick.start;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.constants.Constants;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.devices.events.GetUserUpdates;
import akka.quick.start.devices.events.UpdateDeviceTemperature;
import akka.quick.start.messages.request.RecordTemperature;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.GroupOrDeviceRegistered;
import akka.quick.start.messages.response.TemperatureRecorded;
import akka.quick.start.messages.response.UserResigistered;
import akka.quick.start.users.UserDashBoardManager;
import scala.concurrent.duration.Duration;

/**
 * This starts all the actors and send dummy updates to devices and devices as per configuration file.
 * @author puneethvreddy
 *
 */
public class IotSupervisor extends UntypedActor{

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Map<String, List<String>> groupDeviceList = new HashMap<>();
	private final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
	private final Map<ActorRef, String> actorToDeviceId = new HashMap<>();

	private final Map<String, ActorRef> userIdToActor = new HashMap<>();
	private final Map<ActorRef, String> actorToUserId = new HashMap<>();

	Cancellable deviceTemperatureUpdater;
	Cancellable askUserForTemperatureUpdates;

	private final Config config;
	private ActorRef deviceManager = null; 
	ActorRef userDashBoardManager = null;

	private List<String> deviceList = new ArrayList<>();
	private List<String> userList = new ArrayList<>();
	private List<String> groupList = new ArrayList<>();

	public IotSupervisor(Config config) {
		this.config = config;
	}

	public static Props props(Config config){
		return Props.create(IotSupervisor.class, config);
	}

	@Override
	public void preStart() throws Exception {
		this.loadConfig(config);
		this.printAllData();
		this.deviceManager = getContext().actorOf(DeviceManager.props(),"device-manager");
		this.startDevices(deviceManager);
		//start watching the bridge actor for devices.
		getContext().watch(this.deviceManager);
		//store this reference under devices
		this.deviceIdToActor.put("device-dash-board", this.deviceManager);
		this.actorToDeviceId.put(this.deviceManager,"device-dash-board");

		this.userDashBoardManager = getContext().actorOf(UserDashBoardManager.props(deviceManager),"user-manager");
		this.startUsers(userDashBoardManager);
		//start watching the bridge actor for users.
		getContext().watch(this.userDashBoardManager);
		//store this reference under users.
		this.userIdToActor.put("user-dash-board", this.userDashBoardManager);
		this.actorToUserId.put(this.userDashBoardManager,"user-dash-board");

		//a scheduler to send temperature updates for a random device.
		deviceTemperatureUpdater = getContext().system().scheduler().schedule(
				Duration.apply(2, TimeUnit.SECONDS),
				Duration.apply(1, TimeUnit.SECONDS),
				getSelf(), 
				new UpdateDeviceTemperature(), 
				getContext().dispatcher(), 
				getSelf()
				);

		//a scheduler to ask user for a device temperature updates.
		askUserForTemperatureUpdates = getContext().system().scheduler().schedule(
				Duration.apply(3, TimeUnit.SECONDS),
				Duration.apply(this.deviceList.size(), TimeUnit.SECONDS),
				getSelf(), 
				new GetUserUpdates(), 
				getContext().dispatcher(), 
				getSelf()
				);
	}

	@Override
	public void postStop() {
		this.deviceTemperatureUpdater.cancel();
		this.askUserForTemperatureUpdates.cancel();
	}


	private void onTerminate(ActorRef killedActor,String name, Map<String, ActorRef> idToActor, final Map<ActorRef, String> actorToId){
		actorToId.remove(killedActor);
		idToActor.remove(name);
		if(name.equalsIgnoreCase("device-dash-board") || name.matches("device-dash-board") ){
			log.error("Then main bridging actor {} is killed, can't process any more request. Shutting down system....", name);
			//getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
			getContext().stop(getSelf());
		}
	}

	private void loadConfig(Config config) {
		Config deviceConfig = config.getConfig(Constants.DEVICES_HOME);
		for( Entry<String, ConfigValue> element : deviceConfig.entrySet()){
			String key = element.getKey();
			this.groupDeviceList.put(key, deviceConfig.getStringList(key));
			this.deviceList.addAll(deviceConfig.getStringList(key));
			this.groupList.add(key);
		}
		this.userList = config.getStringList(Constants.USERS_HOME);
	}

	private void printAllData(){
		this.groupDeviceList.keySet().stream().forEach(ele -> {
			System.out.println(ele+ " : "+ this.groupDeviceList.get(ele).stream().collect(Collectors.joining(", ", "[ ", " ]")));
		});

		System.out.println("Users : "+this.userList.stream().collect(Collectors.joining(", ", "[ ", " ]")));
	}

	private void startDevices(ActorRef deviceManager){
		for(String groupName : this.groupDeviceList.keySet()){
			for(String deviceName : this.groupDeviceList.get(groupName)){
				deviceManager.tell(new RegisterGroupOrDevice(generateTokenNumber(), groupName, deviceName), getSelf());
			}
		}
	}

	private void startUsers(ActorRef userDashBoradManager){
		this.userList.stream().forEach(userId ->{
			userDashBoradManager.tell(new RegisterUser(userId, generateTokenNumber()), getSelf());
		} );
	}

	private long generateTokenNumber(){
		Random rand = new Random();
		return rand.nextLong();
	}
	
	private int generateRandomNumber(int max){
		Random rand = new Random();
		return rand.nextInt(max);
	}
	
	@Override
	public void onReceive(Object message) throws Exception {

		if(message instanceof UserResigistered){
			getContext().watch(getSender());
			UserResigistered response = (UserResigistered) message;
			log.info("User {} successfully registered with registration request {}.",response.getUserName(), response.getRequestId());
			this.userIdToActor.put(response.getUserName(), getSender());
			this.actorToUserId.put(getSender(), response.getUserName());
		}else if(message instanceof GroupOrDeviceRegistered){
			getContext().watch(getSender());
			GroupOrDeviceRegistered response = (GroupOrDeviceRegistered) message;
			log.info(
					"Device {} successfully registered under group {} with registration id {}", 
					response.getDeviceId(), 
					response.getGroupId(), 
					response.getRequestId()
					);
			this.deviceIdToActor.put(response.getDeviceId(), getSender());
			this.actorToDeviceId.put(getSender(), response.getDeviceId());
		}else if(message instanceof Terminated){
			ActorRef killedActor = ((Terminated) message).getActor();
			getContext().unwatch(killedActor);
			if(this.actorToDeviceId.containsKey(killedActor)){
				this.onTerminate(killedActor, this.actorToDeviceId.get(killedActor), this.deviceIdToActor, this.actorToDeviceId);
			}else if(this.actorToUserId.containsKey(killedActor)){
				this.onTerminate(killedActor, this.actorToUserId.get(killedActor), this.userIdToActor, this.actorToUserId);
			}
		}else if(message instanceof GetUserUpdates){
			//we can add n elements to this list of groups;
			List<String> groupNames = new ArrayList<>();
			groupNames.add(this.groupList.get(this.generateRandomNumber(this.groupList.size())));
			String requesterName = this.userList.get(this.generateRandomNumber(this.userList.size()));
			this.userDashBoardManager.tell(new RequestAllTemperaturesOfDevicesInGroup(this.generateTokenNumber(), groupNames, requesterName), this.userIdToActor.get(requesterName));
		}else if(message instanceof UpdateDeviceTemperature){
			String deviceName = this.deviceList.get(this.generateRandomNumber(this.deviceList.size()));
			ActorRef deviceRef = this.deviceIdToActor.get(deviceName);
			deviceRef.tell(new RecordTemperature(this.generateTokenNumber(), this.generateRandomNumber(100)), getSelf());
		}else if(message instanceof TemperatureRecorded){
			TemperatureRecorded respose = (TemperatureRecorded) message;
			log.info("Temperature updated for {} with requestId {}", this.actorToDeviceId.get(getSender()), respose.getRequestId());
		}else{
			log.error("Received unknown message{} from {} to {}", message.toString(), getSender().path(), getSelf().path());
		}

	}
}
