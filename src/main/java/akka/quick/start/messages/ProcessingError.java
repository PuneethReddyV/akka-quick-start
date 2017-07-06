package akka.quick.start.messages;

/**
 * This is a message class for actors used for notifying error
 * @author puneethvreddy
 *
 */
public class ProcessingError {

	String error;

	public ProcessingError(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}
	
}
