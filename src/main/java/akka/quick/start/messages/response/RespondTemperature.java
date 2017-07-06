package akka.quick.start.messages.response;

import java.util.Optional;

import akka.quick.start.devices.DeviceGroupQuery;
import akka.quick.start.devices.DeviceWorker;
import akka.quick.start.messages.request.RequestTemperature;

/**
 * This is a reply message to tell the recorded temperature at a device.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is {@link DeviceWorker worker}.<br>
 * Consumer is  {@link DeviceGroupQuery cameo-actor}.<br>
 * Message Type  <b>response</b><br>
 * Response to this request a {@link RequestTemperature request-temperature}.

 * @author puneethvreddy
 *
 */
public final class RespondTemperature {
	long requestId;
	Optional<Double> value;

	public RespondTemperature(long requestId, Optional<Double> value) {
		this.requestId = requestId;
		this.value = value;
	}

	public long getRequestId() {
		return requestId;
	}

	public Optional<Double> getValue() {
		return value;
	}
	
}