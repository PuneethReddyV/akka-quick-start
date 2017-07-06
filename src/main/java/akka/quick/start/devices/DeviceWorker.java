package akka.quick.start.devices;

import java.util.Optional;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.quick.start.messages.request.RequestTemperature;
import akka.quick.start.messages.request.RecordTemperature;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.response.GroupOrDeviceRegistered;
import akka.quick.start.messages.response.RespondTemperature;
import akka.quick.start.messages.response.TemperatureRecorded;


/**
 * A actor of this class takes responsibility of a device, by taking updated temperature from that device it is assigned to.
 * 
 * @author puneethvreddy
 *
 */
public class DeviceWorker extends UntypedActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	final String groupId;

	final String deviceId;

	public DeviceWorker(String groupId, String deviceId) {
		this.groupId = groupId;
		this.deviceId = deviceId;
	}

	public static Props props(String groupId, String deviceId) {
		return Props.create(DeviceWorker.class, groupId, deviceId);
	}


	Optional<Double> lastTemperatureReading = Optional.empty();

	@Override
	public void preStart() {
		log.info("Started device-actor={} in group={}", deviceId, groupId);
	}

	@Override
	public void postStop() {
		log.info("Stopped device-actor={} in group={}", deviceId, groupId);
	}


	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RequestTemperature){
			RequestTemperature r = (RequestTemperature)message;
			getSender().tell(new RespondTemperature(r.getRequestId(), lastTemperatureReading), getSelf());
		}else if( message instanceof RecordTemperature){
			RecordTemperature r = (RecordTemperature) message;
			 log.info("Recorded temperature reading {} with {} by {} ", r.getValue(), r.getRequestId(),this.deviceId);
             lastTemperatureReading = Optional.of(r.getValue());
             getSender().tell(new TemperatureRecorded(r.getRequestId()), getSelf());
		}else if(message instanceof RegisterGroupOrDevice){
			RegisterGroupOrDevice r = (RegisterGroupOrDevice) message;
			if (this.groupId.equals(r.getGroupId()) && this.deviceId.equals(r.getDeviceId())) {
                getSender().tell(new GroupOrDeviceRegistered(r.getRequestId(), r.getGroupId(), r.getDeviceId()), getSelf());
              } else {
                log.warning(
                        "Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
                        r.getGroupId(), r.getDeviceId(), this.groupId, this.deviceId
                );
              }
			
		}else {
			unhandled(message);
		}
	}

}