package akka.quick.start.messages.request;

import java.util.List;

import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.response.RespondDeviceGroupRepresentator;
import akka.quick.start.users.UserDashBoardManager;

/**
 * Request to know all the representatives of a group users.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is {@link UserDashBoardManager user-dash-board-manager}.<br>
 * Consumer is {@link DeviceManager device-manager}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link RespondDeviceGroupRepresentator respond-device-group-representative}.
 * @author puneethvreddy
 *
 */

public class RequestDeviceGroupRepresentator {

	long requestId;
	String requesterName;
	List<String> groupName;

	public RequestDeviceGroupRepresentator(long requestId, String requesterName, List<String> groupName) {
		this.requestId = requestId;
		this.requesterName = requesterName;
		this.groupName = groupName;
	}

	public String getRequesterName() {
		return requesterName;
	}

	public List<String> getGroupName() {
		return groupName;
	}

	public long getRequestId() {
		return requestId;
	}
	

}
