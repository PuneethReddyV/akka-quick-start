package akka.quick.start.messages.response;

import akka.quick.start.users.UserDashBoardManager;
import akka.quick.start.users.UsersDashboard;

/**
 * A reply message used to notify that user is registered.<br>
 * Each request event is identified by the requestId. <br>
 * Source  is {@link UsersDashboard user-dash-board}.<br>
 * Consumer is  {@link UserDashBoardManager user-dash-board-manager}.<br>
 * Message Type  <b>response</b><br>
 * Request to this response a {@link RegisterUser registered-user}.
 * 
 * 
 * @author puneethvreddy
 *
 */
public class UserResigistered {

	private long requestId;
	private String userName;

	public UserResigistered(long requestId, String userName) {
		this.requestId = requestId;
		this.userName = userName;
	}

	public long getRequestId() {
		return requestId;
	}

	public String getUserName() {
		return userName;
	} 
	
	
}
