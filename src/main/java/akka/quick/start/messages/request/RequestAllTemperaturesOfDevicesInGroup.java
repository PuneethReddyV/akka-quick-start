package akka.quick.start.messages.request;

import java.util.List;

import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.DeviceGroupQuery;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.users.UserDashBoardManager;

/**
 * Request event to {@link DeviceGroup device-group-leader} for fetching the temperature from all the active devices.<br>
 * Each request is identified by requestId.<br>
 * This message is forwarded by {@link DeviceGroup device-group} to {@link DeviceGroupQuery cameo-actor}.<br>
 * {@link DeviceGroup device-group-leader} actor creates a {@link DeviceGroupQuery cameo-actor} for each message of this type.<br>
 * Source  is {@link UserDashBoardManager user-dash-board-manager}<br>
 * Consumer is {@link DeviceGroupQuery worker}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link RespondAllTemperaturesOfDevicesInGroup respond-all-temperatures}.
 * @author puneethvreddy
 *
 */
public class RequestAllTemperaturesOfDevicesInGroup {
	
	 final long requestId;
	 final String requesterName;
	 final List<String> groupNames;

	  public RequestAllTemperaturesOfDevicesInGroup(long requestId, List<String> groupNames, String requesterName) {
	    this.requestId = requestId;
	    this.groupNames = groupNames;
	    this.requesterName = requesterName;
	  }

	public List<String> getGroupNames() {
		return this.groupNames;
	}

	public long getRequestId() {
		return this.requestId;
	}

	public String getRequesterName() {
		return this.requesterName;
	}

}
