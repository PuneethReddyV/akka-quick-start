package akka.quick.start.supervisor.devices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.quick.start.devices.DeviceGroup;
import akka.quick.start.devices.DeviceWorker;
import akka.quick.start.messages.request.RequestTemperature;
import akka.quick.start.messages.request.RecordTemperature;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.request.RequestActiveDeviceListOfGroup;
import akka.quick.start.messages.response.GroupOrDeviceRegistered;
import akka.quick.start.messages.response.RespondActiveDeviceListOfGroup;
import akka.quick.start.messages.response.RespondTemperature;
import akka.quick.start.messages.response.TemperatureRecorded;
import akka.testkit.TestKit;
import scala.Function0;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class TestDevice {
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
	public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
	  TestKit probe = new TestKit(system);
	  ActorRef deviceActor = system.actorOf(DeviceWorker.props("group", "device"));
	  deviceActor.tell(new RequestTemperature(42L), probe.testActor());
	  RespondTemperature response = probe.expectMsgClass(RespondTemperature.class);
	  assertEquals(42L, response.getRequestId());
	  assertEquals(Optional.empty(), response.getValue());
	}
	
	@Test
	public void testReplyWithLatestTemperatureReading() {
		ActorSystem system = ActorSystem.create();
		TestKit probe = new TestKit(system);
		ActorRef deviceActor = system.actorOf(DeviceWorker.props("group", "device"),"device-tester");

		deviceActor.tell(new RecordTemperature(1L, 24.0), probe.testActor());
		assertEquals(1L, probe.expectMsgClass(TemperatureRecorded.class).getRequestId());

		deviceActor.tell(new RequestTemperature(2L), probe.testActor());
		RespondTemperature response1 = probe.expectMsgClass(RespondTemperature.class);
		assertEquals(2L, response1.getRequestId());
		assertEquals(Optional.of(24.0), response1.getValue());

		deviceActor.tell(new RecordTemperature(3L, 55.0), probe.testActor());
		assertEquals(3L, probe.expectMsgClass(TemperatureRecorded.class).getRequestId());

		deviceActor.tell(new RequestTemperature(4L), probe.testActor());
		RespondTemperature response2 = probe.expectMsgClass(RespondTemperature.class);
		assertEquals(4L, response2.getRequestId());
		assertEquals(Optional.of(55.0), response2.getValue());
	}

	@Test
	public void testReplyToRegistrationRequests() {
		long requestId = 1l;
		TestKit probe = new TestKit(system);
		ActorRef deviceActor = system.actorOf(DeviceWorker.props("group", "device"), "device-test-registration");

		deviceActor.tell(new RegisterGroupOrDevice(requestId, "group", "device"), probe.testActor());
		GroupOrDeviceRegistered response = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response.getRequestId());
		assertEquals( "group", response.getGroupId());
		assertEquals("device", response.getDeviceId());
		assertEquals(deviceActor, probe.lastSender());
	}

	@Test
	public void testIgnoreWrongRegistrationRequests() {
		long requestId = 2l;
		TestKit probe = new TestKit(system);
		ActorRef deviceActor = system.actorOf(DeviceWorker.props("group", "device"));

		deviceActor.tell(new RegisterGroupOrDevice(requestId, "wrongGroup", "device"), probe.testActor());
		probe.expectNoMsg();

		deviceActor.tell(new RegisterGroupOrDevice(requestId, "group", "wrongDevice"), probe.testActor());
		probe.expectNoMsg();
	}

	@Test
	public void testRegisterDeviceActor() {
		long requestId = 3l;
		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device1"), probe.testActor());
		GroupOrDeviceRegistered response1 = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response1.getRequestId());
		assertEquals( "group", response1.getGroupId());
		assertEquals("device1", response1.getDeviceId());
		ActorRef deviceActor1 = probe.lastSender();

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device2"), probe.testActor());
		GroupOrDeviceRegistered response2 = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response2.getRequestId());
		assertEquals( "group", response2.getGroupId());
		assertEquals("device2", response2.getDeviceId());
		ActorRef deviceActor2 = probe.lastSender();
		assertNotEquals(deviceActor1, deviceActor2);


		// Check that the device actors are workingl
		deviceActor1.tell(new RecordTemperature(0L, 1.0), probe.testActor());
		assertEquals(0L, probe.expectMsgClass(TemperatureRecorded.class).getRequestId());
		deviceActor2.tell(new RecordTemperature(1L, 2.0), probe.testActor());
		assertEquals(1L, probe.expectMsgClass(TemperatureRecorded.class).getRequestId());
	}

	@Test
	public void testIgnoreRequestsForWrongGroupId() {
		long requestId = 4l;
		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new RegisterGroupOrDevice(requestId, "wrongGroup", "device1"), probe.testActor());
		probe.expectNoMsg();
	}

	@Test
	public void testReturnSameActorForSameDeviceId() {
		long requestId = 5l;
		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device1"), probe.testActor());
		GroupOrDeviceRegistered response = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response.getRequestId());
		assertEquals( "group", response.getGroupId());
		assertEquals("device1", response.getDeviceId());
		ActorRef deviceActor1 = probe.lastSender();

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device1"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		ActorRef deviceActor2 = probe.lastSender();
		assertEquals(deviceActor1, deviceActor2);
	}

	@Test
	public void testListActiveDevices() {
		long requestId = 6l;
		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device1"), probe.testActor());
		GroupOrDeviceRegistered response1 = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response1.getRequestId());
		assertEquals( "group", response1.getGroupId());
		assertEquals("device1", response1.getDeviceId());

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device2"), probe.testActor());
		GroupOrDeviceRegistered response2 = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response2.getRequestId());
		assertEquals( "group", response2.getGroupId());
		assertEquals("device2", response2.getDeviceId());

		groupActor.tell(new RequestActiveDeviceListOfGroup(requestId, "group"), probe.testActor());
		RespondActiveDeviceListOfGroup reply = probe.expectMsgClass(RespondActiveDeviceListOfGroup.class);
		assertEquals(requestId, reply.getRequestId());
		assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.getIds());
	}

	@Test
	public void testListActiveDevicesAfterOneShutsDown() {
		long requestId = 7l;
		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device1"), probe.testActor());
		GroupOrDeviceRegistered response1 = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response1.getRequestId());
		assertEquals( "group", response1.getGroupId());
		assertEquals("device1", response1.getDeviceId());
		ActorRef toShutDown = probe.lastSender();

		groupActor.tell(new RegisterGroupOrDevice(requestId, "group", "device2"), probe.testActor());
		GroupOrDeviceRegistered response2 = probe.expectMsgClass(GroupOrDeviceRegistered.class);
		assertEquals(requestId, response2.getRequestId());
		assertEquals( "group", response2.getGroupId());
		assertEquals("device2", response2.getDeviceId());

		groupActor.tell(new RequestActiveDeviceListOfGroup(requestId, "group"), probe.testActor());
		RespondActiveDeviceListOfGroup reply = probe.expectMsgClass(RespondActiveDeviceListOfGroup.class);
		assertEquals(requestId, reply.getRequestId());
		assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.getIds());

		probe.watch(toShutDown);
		toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender());
		probe.expectTerminated(toShutDown, Duration.apply(1, TimeUnit.SECONDS));

		// using awaitAssert to retry because it might take longer for the groupActor
		// to see the Terminated, that order is undefined
		probe.awaitAssert(new Function0<Object>() {
			@Override
			public Object apply() {
				groupActor.tell(new RequestActiveDeviceListOfGroup(requestId, "group"), probe.testActor());
				RespondActiveDeviceListOfGroup r = 
						probe.expectMsgClass(RespondActiveDeviceListOfGroup.class);
				assertEquals(requestId, r.getRequestId());
				assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.getIds());
				return null;
			}
			@Override
			public byte apply$mcB$sp() {
				return 0;
			}
			@Override
			public char apply$mcC$sp() {
				return 0;
			}
			@Override
			public double apply$mcD$sp() {
				return 0;
			}
			@Override
			public float apply$mcF$sp() {
				return 0;
			}
			@Override
			public int apply$mcI$sp() {
				return 0;
			}
			@Override
			public long apply$mcJ$sp() {
				return 0;
			}
			@Override
			public short apply$mcS$sp() {
				return 0;
			}
			@Override
			public void apply$mcV$sp() {
			}
			@Override
			public boolean apply$mcZ$sp() {
				return false;
			}
		}, Duration.apply(2, TimeUnit.SECONDS), Duration.apply(2, TimeUnit.SECONDS));

	}


}
