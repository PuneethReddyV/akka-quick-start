package akka.quick.start.messages.request;

import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.devices.DeviceWorker;
import akka.quick.start.messages.response.GroupOrDeviceRegistered;

/**
 * Request register a group or device with {@link DeviceManager device-manager}.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is <b><i>which ever actor wants to register group or device</i></b>.<br>
 * Consumer is sequence of forward events  {@link DeviceManager device-manager} ->  {@link DeviceGroup device-group} ->  {@link DeviceWorker device-worker}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link GroupOrDeviceRegistered group-or-device-registered}.
 * @author puneethvreddy
 *
 */

public final class RegisterGroupOrDevice {
	final long requestId;
	final String groupId;
	final String deviceId;
	
	public RegisterGroupOrDevice(long requestId, String groupId, String deviceId) {
		this.requestId = requestId;
		this.groupId = groupId;
		this.deviceId = deviceId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public long getRequestId() {
		return requestId;
	}

	public String getGroupId() {
		return groupId;
	}
	
	
	
}