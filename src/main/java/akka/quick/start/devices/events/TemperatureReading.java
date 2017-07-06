package akka.quick.start.devices.events;

/**
 * This is a parent to the following events.
 * <li> {@link Temperature temperature} - when we receive a temperature from a active device. </li>
 * <li> {@link TemperatureNotAvailable temperature-not-available temperature }- what we received is empty response from active device. </li>
 * <li> {@link DeviceTimedOut device-timed-out} - active device didn't respond in time.</li>
 * <li> {@link DeviceNotAvailable device-not-available}- device killed before sending current temperature.</li>
 * @author puneethvreddy
 *
 */
public interface TemperatureReading {

}
