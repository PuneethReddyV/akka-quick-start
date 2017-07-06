package akka.quick.start.messages.response;

import java.util.Map;

import akka.quick.start.devices.DeviceGroupQuery;
import akka.quick.start.devices.events.TemperatureReading;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.users.UsersDashboard;

/**
 * This is a response to tell all the recorded temperatures of all active devices.<br>
 * Source  is {@link DeviceGroupQuery cameo-actor}.<br>
 * Consumer is  {@link UsersDashboard user-dash-board}.<br>
 * Message Type  <b>response</b><br>
 * Response to this request a {@link RequestAllTemperaturesOfDevicesInGroup request-all-temperatures-of-devices-in-group}.
 * 
 * @author puneethvreddy
 *
 */

public class RespondAllTemperaturesOfDevicesInGroup {

	final long requestId;
	  final Map<String, TemperatureReading> temperatures;

	  public RespondAllTemperaturesOfDevicesInGroup(long requestId, Map<String, TemperatureReading> temperatures) {
	    this.requestId = requestId;
	    this.temperatures = temperatures;
	  }

	public long getRequestId() {
		return requestId;
	}

	public Map<String, TemperatureReading> getTemperatures() {
		return temperatures;
	}
	  
}
