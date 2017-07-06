package akka.quick.start.messages.response;

import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.devices.DeviceWorker;

/**
 * Simple event to used for completion of group or devices. Triggered when there is new devices entry is added to a group.
 * Each read event is identified by the requestId. <br>
 * Source  is  {@link DeviceManager device-manager} ->  {@link DeviceGroup device-group} ->  {@link DeviceWorker device-worker}.<br>
 * Consumer is sequence of forward events <b><i>which ever actor wants to register group or device</i></b>.<br>
 * Message Type  <b>response</b><br>
 * Response to this request a {@link RegisterGroupOrDevice register-group-or-device}.
 * 
 * 
 * @author vangalap
 *
 *
 */
public class GroupOrDeviceRegistered {

	final long requestId;
	final String groupId;
	final String deviceId;
	
	public GroupOrDeviceRegistered(long requestId, String groupId, String deviceId) {
		this.requestId = requestId;
		this.groupId = groupId;
		this.deviceId = deviceId;
	}

	public long getRequestId() {
		return requestId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	
}
