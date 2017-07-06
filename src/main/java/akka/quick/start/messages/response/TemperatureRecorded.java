package akka.quick.start.messages.response;

import akka.quick.start.devices.DeviceWorker;
import akka.quick.start.messages.request.RecordTemperature;

/**
 * This is a reply message to notify temperature is recorded.<br>
 * Source  is {@link DeviceWorker worker}.<br>
 * Consumer is  <b><i>any device to worker actor is linked</i></b>.<br>
 * Message Type  <b>response</b><br>
 * Request to this response is {@link RecordTemperature record-temperature}.
 * 
 * @author puneethvreddy
 *
 */
public class TemperatureRecorded {
	final long requestId;

	public TemperatureRecorded(long requestId) {
		this.requestId = requestId;
	}

	public long getRequestId() {
		return requestId;
	}

}
