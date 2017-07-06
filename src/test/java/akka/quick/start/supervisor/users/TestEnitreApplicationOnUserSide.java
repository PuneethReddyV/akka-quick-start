package akka.quick.start.supervisor.users;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.quick.start.devices.DeviceGroupQuery;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.request.RequestDeviceGroupRepresentator;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.RespondDeviceGroupRepresentator;
import akka.quick.start.messages.response.UserResigistered;
import akka.quick.start.users.UserDashBoardManager;
import akka.testkit.TestKit;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class TestEnitreApplicationOnUserSide {

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
	public void testCase(){
		long requestId = 1L;
		TestKit probeForDevices = new TestKit(system);
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(probeForDevices.testActor()), "user-manager"+requestId);

		//register a user.
		userManager.tell(new RegisterUser("puneeth", requestId), probeForDevices.testActor());
		probeForDevices.expectMsgClass(UserResigistered.class);

		//request for updated temperatures
		List<String> groupNames = new ArrayList<>();
		groupNames.add("group-1");
		userManager.tell(new RequestAllTemperaturesOfDevicesInGroup(requestId, groupNames, "puneeth"), probeForDevices.testActor());
		
		//verify request for device group representatives
		RequestDeviceGroupRepresentator request = probeForDevices.expectMsgClass(RequestDeviceGroupRepresentator.class);
		assertEquals(requestId, request.getRequestId());
		assertEquals(Stream.of("group-1").collect(Collectors.toList()), request.getGroupName());
		assertEquals("puneeth", request.getRequesterName());
		
		//send the dummy response for device group representatives
		Set<ActorRef> activeGroup = new HashSet<>();
		activeGroup.add(probeForDevices.testActor());
		userManager.tell(new RespondDeviceGroupRepresentator(requestId, activeGroup, request.getRequesterName(), request.getGroupName()), probeForDevices.testActor());
		
		//response for device group representatives
		RequestAllTemperaturesOfDevicesInGroup responseGroupRepresentative = probeForDevices.expectMsgClass(RequestAllTemperaturesOfDevicesInGroup.class);
		assertEquals(requestId, responseGroupRepresentative.getRequestId());
		assertEquals(Stream.of("group-1").collect(Collectors.toList()), responseGroupRepresentative.getGroupNames());
		assertEquals("puneeth", responseGroupRepresentative.getRequesterName());
		

		// Create DeviceGroupQuery actor and to get DeviceTimedOut timer event.
		system.actorOf(
				DeviceGroupQuery.props(
						new HashMap<>(), requestId, probeForDevices.testActor(), Duration.create(2, TimeUnit.SECONDS)
						),
				"query-group-actor"
				);

		//verify response from DeviceGroupQuery.
		RespondAllTemperaturesOfDevicesInGroup response = probeForDevices.expectMsgClass(RespondAllTemperaturesOfDevicesInGroup.class);
		assertEquals(response.getRequestId(), requestId);
		assertEquals(response.getTemperatures().size(), 0);

	}
}
