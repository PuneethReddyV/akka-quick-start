package akka.quick.start.messages.response;

import java.util.Set;

import akka.actor.ActorRef;
import akka.quick.start.users.UserDashBoardManager;

/**
 * 
 * Actor message used for requesting all active user list.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is {@link UserDashBoardManager user-dash-board-manager}.<br>
 * Consumer is <b><i>Who ever whats to know the active users</i></b>.<br>
 * Message Type  <b>response</b><br>
 * Request to this response is {@link RequestActiveUserList request-active-user-list}.
 * 
 * @author puneethvreddy
 *
 */
public class RespondActiveUserList {

	long requestId;
	Set<ActorRef> activeUserList;

	public RespondActiveUserList(long requestId, Set<ActorRef> activeUserList) {
		this.requestId = requestId;
		this.activeUserList = activeUserList;
	} 
	
	public long getRequestId() {
		return requestId;
	}
	public Set<ActorRef> getActiveUserList() {
		return activeUserList;
	}
	
}
