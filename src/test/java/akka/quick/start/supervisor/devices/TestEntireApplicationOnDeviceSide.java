package akka.quick.start.supervisor.devices;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.events.Temperature;
import akka.quick.start.devices.events.TemperatureNotAvailable;
import akka.quick.start.devices.events.TemperatureReading;
import akka.quick.start.messages.request.RecordTemperature;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.GroupOrDeviceRegistered;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.TemperatureRecorded;
import akka.testkit.TestKit;
import scala.concurrent.duration.FiniteDuration;

public class TestEntireApplicationOnDeviceSide {
	
	static ActorSystem system;
	@BeforeClass
	public static void setup() {
		system = ActorSystem.create();
	}

	@AfterClass
	public static void teardown() {
		TestKit.shutdownActorSystem(system, FiniteDuration.create(1, TimeUnit.SECONDS), false);
		system = null;
	}

	@Test
	public void testCollectTemperaturesFromAllActiveDevices() {
		long requestId = 6L;

		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device1"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		ActorRef deviceActor1 = probe.lastSender();

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device2"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		ActorRef deviceActor2 = probe.lastSender();

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device3"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		@SuppressWarnings("unused")
		ActorRef deviceActor3 = probe.lastSender();

		// Check that the device actors are working
		deviceActor1.tell(new RecordTemperature(requestId, 1.0), probe.testActor());
		assertEquals(requestId, probe.expectMsgClass(TemperatureRecorded.class).getRequestId());
		deviceActor2.tell(new RecordTemperature(requestId, 2.0), probe.testActor());
		assertEquals(requestId, probe.expectMsgClass(TemperatureRecorded.class).getRequestId());
		// No temperature for device 3

		List<String> groupNames = new ArrayList<>();
		groupNames.add("group");
		groupActor.tell(new RequestAllTemperaturesOfDevicesInGroup(requestId, groupNames, "test-user"), probe.testActor());
		RespondAllTemperaturesOfDevicesInGroup response = probe.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, response.getRequestId());

		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new Temperature(2.0));
		expectedTemperatures.put("device3", new TemperatureNotAvailable());

		for (String key : expectedTemperatures.keySet()) {
			assertEquals(expectedTemperatures.get(key).getClass(), response.getTemperatures().get(key).getClass());
			if(!key.equalsIgnoreCase("device3"))
				assertEquals(((Temperature)expectedTemperatures.get(key)).getValue(), ((Temperature)response.getTemperatures().get(key)).getValue(), 0.0);
		}
	}
}
