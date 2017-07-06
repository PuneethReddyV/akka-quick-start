package akka.quick.start.messages.request;

import akka.quick.start.messages.response.UserResigistered;
import akka.quick.start.users.UserDashBoardManager;
import akka.quick.start.users.UsersDashboard;

/**
 * Register device event to link {@link UsersDashboard user-dash-board} class to a actor.<br>
 * Each request event is identified by the requestId. <br>
 * Source  is {@link UserDashBoardManager user-dash-board-manager}.<br>
 * Consumer is {@link UsersDashboard user-dash-board}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link UserResigistered user-registered}.
 * @author puneethvreddy
 *
 */

public class RegisterUser {
	
	private String userName;
	private long requestId;
	
	public RegisterUser(String userName, long requestId) {
		this.userName = userName;
		this.requestId = requestId;
	}

	public long getRequestId() {
		return requestId;
	}

	public String getUserName() {
		return userName;
	}
	
}
