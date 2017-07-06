package akka.quick.start.supervisor.users;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
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
import akka.actor.PoisonPill;
import akka.quick.start.devices.DeviceManager;
import akka.quick.start.messages.ProcessingError;
import akka.quick.start.messages.request.RegisterUser;
import akka.quick.start.messages.request.RequestActiveUserList;
import akka.quick.start.messages.request.RequestAllTemperaturesOfDevicesInGroup;
import akka.quick.start.messages.request.RequestDeviceGroupRepresentator;
import akka.quick.start.messages.response.RespondActiveUserList;
import akka.quick.start.messages.response.RespondDeviceGroupRepresentator;
import akka.quick.start.messages.response.UserResigistered;
import akka.quick.start.users.UserDashBoardManager;
import akka.testkit.TestKit;
import scala.Function0;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class TestUserDashBoardManager {
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

	/**
	 * Negative test case for SECOND message  {@link UserDashBoardManager}
	 */
	
	@Test
	public void testWrongUserNameGetError(){
		long requestId = 1L;
		TestKit probe = new TestKit(system);
		ActorRef deviceSideManager = system.actorOf(DeviceManager.props(), "device-manager"+requestId);
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(deviceSideManager), "user-manager"+requestId);
		List<String> groups = new ArrayList<>();
		groups.add("*");
		userManager.tell(new RequestAllTemperaturesOfDevicesInGroup(requestId, groups, "unknownUser"), probe.testActor());
		probe.expectMsgClass(ProcessingError.class);
	}
	
	/**
	 * Positive test case for SECOND message  {@link UserDashBoardManager}
	 */
	
	
	@Test
	public void testRegisteredUserDontGetErrorForRegisteredDevices(){
		long requestId = 2L;
		TestKit probeForDevices = new TestKit(system);
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(probeForDevices.testActor()), "user-manager"+requestId);
		userManager.tell(new RegisterUser("puneeth", requestId), probeForDevices.testActor());
		
		UserResigistered userResigistered = probeForDevices.expectMsgClass(UserResigistered.class);
		assertEquals(requestId, userResigistered.getRequestId());
		
		List<String> groups = new ArrayList<>();
		groups.add("*");
		userManager.tell(new RequestAllTemperaturesOfDevicesInGroup(requestId, groups, "puneeth"), probeForDevices.testActor());
		RequestDeviceGroupRepresentator request = probeForDevices.expectMsgClass(RequestDeviceGroupRepresentator.class);
		
		assertEquals(request.getGroupName(), Stream.of("*").collect(Collectors.toList()));
		assertEquals(request.getRequesterName(), "puneeth");
		assertEquals(request.getRequestId(), requestId);
		
	}
	
	/**
	 * Negative test case for THIRD message  {@link UserDashBoardManager}
	 */
	
	@Test
	public void testRespondDeviceGroupRepresentatorWithUnknownUser(){
		long requestId = 3L;
		TestKit probeForDevices = new TestKit(system);
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(probeForDevices.testActor()), "user-manager"+requestId);
		List<String> groups = new ArrayList<>();
		groups.add("*");
		userManager.tell(new RespondDeviceGroupRepresentator(requestId, new HashSet<>(), "dummy-actor", groups), probeForDevices.testActor());
		probeForDevices.expectMsgClass(ProcessingError.class);
	}
	
	/**
	 * Positive test case for THIRD message "if" block {@link UserDashBoardManager}
	 */
	@Test
	public void testRespondDeviceGroupRepresentatorWithEmptyDeviceGroup(){

		long requestId = 5L;
		TestKit probeForDevices = new TestKit(system);
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(probeForDevices.testActor()), "user-manager"+requestId);
		userManager.tell(new RegisterUser("puneeth", requestId), probeForDevices.testActor());
		probeForDevices.expectMsgClass(UserResigistered.class);
		
		List<String> groups = new ArrayList<>();
		groups.add("*");
		userManager.tell(new RespondDeviceGroupRepresentator(requestId, null, "puneeth", groups), probeForDevices.testActor());
		probeForDevices.expectMsgClass(ProcessingError.class);
	
		userManager.tell(new RespondDeviceGroupRepresentator(requestId, new HashSet<>(), "puneeth", groups), probeForDevices.testActor());
		probeForDevices.expectMsgClass(ProcessingError.class);
	}
	
	/**
	 * Positive test case for THIRD message "if else" block {@link UserDashBoardManager}
	 */
	
	@Test
	public void testRespondDeviceGroupRepresentatorWithKnownUser(){
		long requestId = 4L;
		TestKit probeForDevices = new TestKit(system);
		ActorRef userManager = system.actorOf(UserDashBoardManager.props(probeForDevices.testActor()), "user-manager"+requestId);
		userManager.tell(new RegisterUser("puneeth", requestId), probeForDevices.testActor());
		probeForDevices.expectMsgClass(UserResigistered.class);
		
		Set<ActorRef> input = new HashSet<>();
		input.add(probeForDevices.testActor());
		List<String> groups = new ArrayList<>();
		groups.add("*");
		userManager.tell(new RespondDeviceGroupRepresentator(requestId, input, "puneeth", groups), probeForDevices.testActor());
		
		RequestAllTemperaturesOfDevicesInGroup response = probeForDevices.expectMsgClass(RequestAllTemperaturesOfDevicesInGroup.class);
		assertEquals(response.getGroupNames(), Stream.of("*").collect(Collectors.toList()));
		assertEquals(requestId, response.getRequestId());
		assertEquals("puneeth", response.getRequesterName());
	}

	/**
	 *  test case for FOURTH message, FIFTH message is also included in this case.
	 */
	
	public void testActiveUserListAfterKillingOneUserActor(){
		long requestId = 4L;
		TestKit probe = new TestKit(system);
		ActorRef userGroupActor = system.actorOf(UserDashBoardManager.props(probe.testActor()));

		userGroupActor.tell(new RegisterUser("actor1", requestId), probe.testActor());
		probe.expectMsgClass(UserResigistered.class);
		ActorRef toShutDown = probe.lastSender();
		
		userGroupActor.tell(new RegisterUser("actor2", requestId), probe.testActor());
		probe.expectMsgClass(UserResigistered.class);

		userGroupActor.tell(new RequestActiveUserList(requestId), probe.testActor());
		RespondActiveUserList reply = probe.expectMsgClass(RespondActiveUserList.class);
		assertEquals(requestId, reply.getRequestId());
		assertEquals(Stream.of("actor1", "actor2").collect(Collectors.toSet()), reply.getActiveUserList());

		probe.watch(toShutDown);
		toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender());
		probe.expectTerminated(toShutDown, Duration.apply(1, TimeUnit.SECONDS));

		probe.awaitAssert(new Function0<Object>() {
			@Override
			public Object apply() {
				userGroupActor.tell(new RequestActiveUserList(requestId), probe.testActor());
				RespondActiveUserList r = 
						probe.expectMsgClass(RespondActiveUserList.class);
				assertEquals(requestId, r.getRequestId());
				assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.getActiveUserList());
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

