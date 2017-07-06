package akka.quick.start.messages.request;

import akka.quick.start.devices.DeviceWorker;
import akka.quick.start.messages.response.TemperatureRecorded;

/**
 * Records temperature is a message event for {@link DeviceWorker worker} class.<br>
 * Each message is uniquely identified by a requestId.<br>
 * Source  is <b><i>any device linked to worker actor</i></b>.<br>
 * Consumer is {@link DeviceWorker worker}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link TemperatureRecorded temperature-recorded}.
 * @author puneethvreddy
 *
 */
public final class RecordTemperature {
	final long requestId;
	final double value;

	public RecordTemperature(long requestId, double value) {
		this.requestId = requestId;
		this.value = value;
	}

	public long getRequestId() {
		return requestId;
	}

	public double getValue() {
		return value;
	}
	
}