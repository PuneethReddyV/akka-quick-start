package akka.quick.start.devices.events;

/**
 * This is response event ( when we receive a temperature from a active device ) for a request of current temperature reading.
 * @author puneethvreddy
 *
 */
public class Temperature implements TemperatureReading {
	public final double value;

	public Temperature(double value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

}
