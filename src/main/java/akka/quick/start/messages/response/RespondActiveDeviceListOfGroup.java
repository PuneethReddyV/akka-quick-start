package akka.quick.start.messages.response;

import java.util.Set;

import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.request.RequestActiveDeviceListOfGroup;
import akka.quick.start.users.UserDashBoardManager;

/**
* Response message used to know which are all the active device actors.<br>
* Source  is {@link DeviceGroup Device Group}.<br>
* Consumer is {@link DeviceManager device-manager} or {@link UserDashBoardManager user-dash-board-manager}<br>
* Message Type  <b>response</b><br>
* 
* Request to this response a {@link RequestActiveDeviceListOfGroup request-active-device-list-of-group}.
*/

public final class RespondActiveDeviceListOfGroup {
  final long requestId;
  final Set<String> ids;

  public RespondActiveDeviceListOfGroup(long requestId, Set<String> ids) {
    this.requestId = requestId;
    this.ids = ids;
  }

public long getRequestId() {
	return requestId;
}

public Set<String> getIds() {
	return ids;
}
  
}