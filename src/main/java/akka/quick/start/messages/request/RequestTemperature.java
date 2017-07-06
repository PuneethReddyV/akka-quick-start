package akka.quick.start.messages.request;

import akka.quick.start.devices.DeviceGroupQuery;
import akka.quick.start.devices.DeviceWorker;
import akka.quick.start.messages.response.RespondTemperature;

/**
 * Reads temperature is a message event for a {@link DeviceWorker worker} class.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is {@link DeviceGroupQuery cameo-actor}.<br>
 * Consumer is {@link DeviceWorker worker}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link RespondTemperature respondTemperature}.
 * @author puneethvreddy
 *
 */
public final class RequestTemperature {
	long requestId;

	public RequestTemperature(long requestId) {
		this.requestId = requestId;
	}

	public long getRequestId() {
		return requestId;
	}
	
}