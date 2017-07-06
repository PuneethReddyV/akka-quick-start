package akka.quick.start.messages.request;

import akka.quick.start.messages.response.RespondActiveUserList;
import akka.quick.start.users.UserDashBoardManager;

/**
 * Request to know all the active users.<br>
 * Each read event is identified by the requestId. <br>
 * Source  is <b><i>Who ever whats to know the active users</i></b>.<br>
 * Consumer is {@link UserDashBoardManager user-dash-board-manager}.<br>
 * Message Type  <b>request</b><br>
 * Response to this request a {@link RespondActiveUserList respond-active-user-list}.
 * @author puneethvreddy
 *
 */

public class RequestActiveUserList {

	long resquestId;
	
	public RequestActiveUserList(long resquestId) {
		super();
		this.resquestId = resquestId;
	}

	public long getResquestId() {
		return resquestId;
	}

}