package akka.quick.start.messages.response;

import java.util.List;
import java.util.Set;

import akka.actor.ActorRef;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.users.UserDashBoardManager;

/**
 * Response to tell all the representatives of a group users.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is {@link DeviceManager device-manager}.<br>
 * Consumer is {@link UserDashBoardManager user-dash-board-manager}.<br>
 * Message Type  <b>response</b><br>
 * Request to this response is {@link RequestDeviceGroupRepresentator request-device-group-representative}.
 * @author puneethvreddy
 *
 */
public class RespondDeviceGroupRepresentator {
	long requestId;
	private Set<ActorRef> listOfGroupActors;
	String requesterName;
	List<String> groupName;

	public RespondDeviceGroupRepresentator(long requestId, Set<ActorRef> listOfGroupActors, String requesterName, List<String> groupName) {
		this.requestId = requestId;
		this.listOfGroupActors = listOfGroupActors;
		this.requesterName = requesterName;
		this.groupName = groupName;
	}

	public Set<ActorRef> getListOfGroupActors() {
		return this.listOfGroupActors;
	}
	
	public String getRequesterName(){
		return this.requesterName;
	}
	
	public List<String>  getGroupName(){
		return this.groupName;
	}

	public long getRequestId() {
		return requestId;
	}
	
	
}
