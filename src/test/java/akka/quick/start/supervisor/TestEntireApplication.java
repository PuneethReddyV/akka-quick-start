package akka.quick.start.supervisor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.request.RecordTemperature;
import akka.quick.start.messages.request.RegisterGroupOrDevice;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.GroupOrDeviceRegistered;
import akka.quick.start.messages.response.TemperatureRecorded;
import akka.quick.start.messages.response.UserResigistered;
import akka.quick.start.users.UserDashBoardManager;
import akka.testkit.TestKit;
import scala.concurrent.duration.FiniteDuration;

public class TestEntireApplication {
	
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
	public void testCaseForEnitreApplication() {

		long requestId = 1L;
		TestKit probe = new TestKit(system);
		TestKit probeForDevices = new TestKit(system);
		
		//create device manager
		ActorRef deviceManager = system.actorOf(DeviceManager.props(),"device-manager");
		
		//create device-1 in group
		deviceManager.tell(new RegisterGroupOrDevice(requestId, "group", "1"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		ActorRef deviceActor1 = probe.lastSender();

		//create device-2 in group
		deviceManager.tell(new RegisterGroupOrDevice(requestId, "group", "2"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		ActorRef deviceActor2 = probe.lastSender();

		//create device-3 in group
		deviceManager.tell(new RegisterGroupOrDevice(requestId, "group", "3"), probe.testActor());
		probe.expectMsgClass(GroupOrDeviceRegistered.class);
		@SuppressWarnings("unused")
		ActorRef deviceActor3 = probe.lastSender();

		// Check that the device actors are working
		deviceActor1.tell(new RecordTemperature(requestId, 1.0), probe.testActor());
		probe.expectMsgClass(TemperatureRecorded.class);
		deviceActor2.tell(new RecordTemperature(requestId, 2.0), probe.testActor());
		probe.expectMsgClass(TemperatureRecorded.class);
		// No temperature for device 3

		//User side 
		
		//create a manager
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(deviceManager), "user-manager"+requestId);

		//register a user.
		userManager.tell(new RegisterUser("puneeth", requestId), probeForDevices.testActor());
		probeForDevices.expectMsgClass(UserResigistered.class);

		//request for updated temperatures
		List<String> groupNames = new ArrayList<>();
		groupNames.add("group");
		userManager.tell(new RequestAllTemperaturesOfDevicesInGroup(requestId, groupNames, "puneeth"), probeForDevices.testActor());
		probeForDevices.expectNoMsg();
	}
		
}
