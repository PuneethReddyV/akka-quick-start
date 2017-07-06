package akka.quick.start.messages.request;

import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.response.RespondActiveDeviceListOfGroup;
import akka.quick.start.users.UserDashBoardManager;

/**
 * Request message used to know which are all the active device of a group.<br>
 * Source {@link DeviceManager device-manager} or {@link UserDashBoardManager user-dash-board-manager}<br>
 * Consumer is {@link DeviceGroup Device Group}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link RespondActiveDeviceListOfGroup reply-active-device-list}.
 * @author puneethvreddy
 */

public class RequestActiveDeviceListOfGroup {
	public final long requestId;
	public final String groupId;

	public RequestActiveDeviceListOfGroup(long requestId, String groupId) {
		this.requestId = requestId;
		this.groupId = groupId;
	}

}
