package akka.quick.start.supervisor.devices;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.quick.start.devices.DeviceGroupQuery;
import akka.quick.start.devices.events.DeviceNotAvailable;
import akka.quick.start.devices.events.DeviceTimedOut;
import akka.quick.start.devices.events.Temperature;
import akka.quick.start.devices.events.TemperatureNotAvailable;
import akka.quick.start.devices.events.TemperatureReading;
import akka.quick.start.messages.request.RequestTemperature;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.RespondTemperature;
import akka.testkit.TestKit;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class DeviceGroupQery {

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
	public void testReturnTemperatureValueForWorkingDevices() {
		long requestId = 1L;
		TestKit requester = new TestKit(system);

		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.testActor(), "device1");
		actorToDeviceId.put(device2.testActor(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				requestId,
				requester.testActor(),
				new FiniteDuration(3L, TimeUnit.SECONDS)));

		assertEquals(requestId, device1.expectMsgClass(RequestTemperature.class).getRequestId());
		assertEquals(requestId, device2.expectMsgClass(RequestTemperature.class).getRequestId());


		queryActor.tell(new RespondTemperature(requestId, Optional.of(2.0)), device2.testActor());
		queryActor.tell(new RespondTemperature(requestId, Optional.of(1.0)), device1.testActor());

		RespondAllTemperaturesOfDevicesInGroup response = requester.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, response.getRequestId());

		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new Temperature(2.0));
		for (String key : expectedTemperatures.keySet()) {
			assertEquals(expectedTemperatures.get(key).getClass(), response.getTemperatures().get(key).getClass());
			assertEquals(((Temperature)expectedTemperatures.get(key)).getValue(), ((Temperature)response.getTemperatures().get(key)).getValue(), 0.0);
		}
	}

	@Test
	public void testReturnTemperatureNotAvailableForDevicesWithNoReadings() {
		long requestId = 2L;
		TestKit requester = new TestKit(system);

		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.testActor(), "device1");
		actorToDeviceId.put(device2.testActor(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				requestId,
				requester.testActor(),
				new FiniteDuration(3, TimeUnit.SECONDS)));

		assertEquals(requestId, device1.expectMsgClass(RequestTemperature.class).getRequestId());
		assertEquals(requestId, device2.expectMsgClass(RequestTemperature.class).getRequestId());

		queryActor.tell(new RespondTemperature(requestId, Optional.empty()), device1.testActor());
		queryActor.tell(new RespondTemperature(requestId, Optional.of(2.0)), device2.testActor());

		RespondAllTemperaturesOfDevicesInGroup response = requester.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, response.getRequestId());

		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new TemperatureNotAvailable());
		expectedTemperatures.put("device2", new Temperature(2.0));

		for (String key : expectedTemperatures.keySet()) {
			assertEquals(expectedTemperatures.get(key).getClass(), response.getTemperatures().get(key).getClass());
			if(!key.equalsIgnoreCase("device1"))
				assertEquals(((Temperature)expectedTemperatures.get(key)).getValue(), ((Temperature)response.getTemperatures().get(key)).getValue(), 0.0);
		}
	}

	@Test
	public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering(){
		long requestId = 3L;

		TestKit sender = new TestKit(system);

		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorRefToDeviceId = new HashMap<>();
		actorRefToDeviceId.put(device1.testActor(), "device1");
		actorRefToDeviceId.put(device2.testActor(), "device2");

		ActorRef queryActor = system.actorOf(
				DeviceGroupQuery.props(actorRefToDeviceId, requestId, sender.testActor(), Duration.create(1L, TimeUnit.SECONDS)), "query-actor");

		assertEquals(requestId , device1.expectMsgClass(RequestTemperature.class).getRequestId());
		assertEquals(requestId , device2.expectMsgClass(RequestTemperature.class).getRequestId());

		system.stop(device1.testActor());
		queryActor.tell(new RespondTemperature(requestId, Optional.of(2.0)), device2.testActor());

		RespondAllTemperaturesOfDevicesInGroup response = sender.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, response.getRequestId());

		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceNotAvailable());
		expectedTemperatures.put("device2", new Temperature(2.0));

		for (String key : expectedTemperatures.keySet()) {
			assertEquals(expectedTemperatures.get(key).getClass(), response.getTemperatures().get(key).getClass());
			if(!key.equalsIgnoreCase("device1"))
				assertEquals(((Temperature)expectedTemperatures.get(key)).getValue(), ((Temperature)response.getTemperatures().get(key)).getValue(), 0.0);
		}
	}

	@Test
	public void testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering(){
		long requestId = 4L;

		TestKit sender = new TestKit(system);

		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorRefToDeviceId = new HashMap<>();
		actorRefToDeviceId.put(device1.testActor(), "device1");
		actorRefToDeviceId.put(device2.testActor(), "device2");

		ActorRef queryActor = system.actorOf(
				DeviceGroupQuery.props(actorRefToDeviceId, requestId, sender.testActor(), Duration.create(1L, TimeUnit.SECONDS)), "query-actor");

		assertEquals(requestId , device1.expectMsgClass(RequestTemperature.class).getRequestId());
		assertEquals(requestId , device2.expectMsgClass(RequestTemperature.class).getRequestId());

		queryActor.tell(new RespondTemperature(requestId, Optional.of(1.0)), device1.testActor());
		queryActor.tell(new RespondTemperature(requestId, Optional.of(2.0)), device2.testActor());
		system.stop(device1.testActor());

		RespondAllTemperaturesOfDevicesInGroup response = sender.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, response.getRequestId());

		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new Temperature(2.0));

		for (String key : expectedTemperatures.keySet()) {
			assertEquals(expectedTemperatures.get(key).getClass(), response.getTemperatures().get(key).getClass());
			assertEquals(((Temperature)expectedTemperatures.get(key)).getValue(), ((Temperature)response.getTemperatures().get(key)).getValue(), 0.0);
		}
	}
	
	@Test
	public void testReturnDeviceTimedOutIfDeviceDoesNotAnswerInTime(){
		long requestId = 5L;

		TestKit sender = new TestKit(system);

		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorRefToDeviceId = new HashMap<>();
		actorRefToDeviceId.put(device1.testActor(), "device1");
		actorRefToDeviceId.put(device2.testActor(), "device2");

		ActorRef queryActor = system.actorOf(
				DeviceGroupQuery.props(actorRefToDeviceId, requestId, sender.testActor(), Duration.create(1L, TimeUnit.SECONDS)), "query-actor");

		assertEquals(requestId , device1.expectMsgClass(RequestTemperature.class).getRequestId());
		assertEquals(requestId , device2.expectMsgClass(RequestTemperature.class).getRequestId());

		queryActor.tell(new RespondTemperature(requestId, Optional.of(1.0)), device1.testActor());

		RespondAllTemperaturesOfDevicesInGroup response = sender.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, response.getRequestId());

		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceTimedOut());

		for (String key : expectedTemperatures.keySet()) {
			assertEquals(expectedTemperatures.get(key).getClass(), response.getTemperatures().get(key).getClass());
			if(key.equalsIgnoreCase("device1"))
			assertEquals(((Temperature)expectedTemperatures.get(key)).getValue(), ((Temperature)response.getTemperatures().get(key)).getValue(), 0.0);
		}
	
		
	}

}
