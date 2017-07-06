package akka.quick.start.supervisor.users;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.request.RequestActiveUserList;
import akka.quick.start.messages.response.RespondActiveUserList;
import akka.quick.start.messages.response.RespondAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.response.UserResigistered;
import akka.quick.start.users.UserDashBoardManager;
import akka.quick.start.users.UsersDashboard;
import akka.testkit.TestKit;
import scala.concurrent.duration.FiniteDuration;

public class TestUsersDashboard {

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
	public void testUserDeviceRegistrationReply(){
		long requestId = 1L;
		TestKit probe = new TestKit(system);
		ActorRef dummyActor = system.actorOf(UsersDashboard.props("puneeth"), "dummy-actor"+requestId);
		dummyActor.tell(new RegisterUser("puneeth", requestId), probe.testActor());

		UserResigistered response =  probe.expectMsgClass(UserResigistered.class);
		assertEquals(requestId , response.getRequestId());
		assertEquals(dummyActor, probe.lastSender());
	}

	@Test
	public void testUserDeviceRegistrationThroughManager(){
		long requestId = 2L;
		TestKit probe = new TestKit(system);
		ActorRef deviceSideManager = system.actorOf(DeviceManager.props(), "device-manager");
		ActorRef userManager = system	.actorOf(UserDashBoardManager.props(deviceSideManager), "user-manager");
		userManager.tell(new RegisterUser("puneeth", requestId), probe.testActor());
		UserResigistered response = probe.expectMsgClass(UserResigistered.class);
		assertEquals(requestId, response.getRequestId());
		ActorRef dummyUserActor = probe.lastSender();
		userManager.tell(new RequestActiveUserList(requestId), probe.testActor());
		RespondActiveUserList response2 = probe.expectMsgClass(RespondActiveUserList.class);
		Optional<ActorRef> activeUser = response2.getActiveUserList().parallelStream().findFirst();
		assertEquals(1, response2.getActiveUserList().size());
		assertEquals(activeUser.get(), dummyUserActor);
	}

	@Test
	public void testRespondAllTemperaturesOfDevicesInGroup(){
		long requestId = 3L;
		TestKit probe = new TestKit(system);
		ActorRef dummyActor = system.actorOf(UsersDashboard.props("puneeth"), "dummy-actor"+requestId);
		dummyActor.tell(new RespondAllTemperaturesOfDevicesInGroup(requestId, new HashMap<>()), probe.testActor());
		probe.expectNoMsg();

	}
}
