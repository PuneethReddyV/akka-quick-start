package akka.quick.start.users;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.devices.events.DeviceNotAvailable;
import akka.quick.start.devices.events.DeviceTimedOut;
import akka.quick.start.devices.events.Temperature;
import akka.quick.start.devices.events.TemperatureNotAvailable;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.UserResigistered;


/**
 * This actor represents a user dash board and display collected temperature from all devices of group(s). 
 * @author puneethvreddy
 *
 */
public class UsersDashboard extends UntypedActor{

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private String userName;

	public UsersDashboard(String userName) {
		this.userName = userName;
	}

	@Override
	public void preStart() {
		log.info("UsersDashboard for \"{}\" started", this.userName);
	}

	@Override
	public void postStop() {
		log.info("UsersDashboard for \"{}\" stopped", this.userName);
	}

	public static Props props(String userName){
		return Props.create(UsersDashboard.class, userName);
	}

	private void toRespondAllTemperatures(RespondAllTemperaturesOfDevicesInGroup response){
		log.info("This is {} reporting for RequestId {}, response details... ",this.userName, response.getRequestId(), response.getRequestId());

		if(response.getTemperatures() != null){
			for(String deviceId : response.getTemperatures().keySet()){
				if(response.getTemperatures().get(deviceId) instanceof Temperature){
					System.out.println(deviceId+" - "+((Temperature)response.getTemperatures().get(deviceId)).value);
				}else if(response.getTemperatures().get(deviceId) instanceof TemperatureNotAvailable){
					System.err.println(deviceId+" - "+0.0);
				}else if(response.getTemperatures().get(deviceId) instanceof DeviceTimedOut){
					System.err.println(deviceId+" - didn't respond on time.");
				}else if(response.getTemperatures().get(deviceId) instanceof DeviceNotAvailable){
					System.err.println(deviceId+" - device is not present. Please bring the device online.");
				}
			}
		}else{
			log.error("Received a null list from {} while processing all the device's temperatures.", getSender());
		}
		
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterUser){
			RegisterUser userDetails = (RegisterUser) message;
			getSender().tell(new UserResigistered(userDetails.getRequestId(), userDetails.getUserName()), getSelf());
		}else if(message instanceof RespondAllTemperaturesOfDevicesInGroup){
			this.toRespondAllTemperatures((RespondAllTemperaturesOfDevicesInGroup) message);
		}else{
			log.error("Received unknown message -{}- from -{}- to -{}.", message.toString(), getSender(), getSelf());
		}
	}

}
