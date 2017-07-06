package akka.quick.start;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;

public class Application
{
	public static final Application _INSTANCE = new Application();
	private static final ActorSystem SYSTEM = ActorSystem.create("system");
	
	public static void main( String[] args )
	{
		Config config = ConfigFactory.load();
		
		SYSTEM.actorOf(IotSupervisor.props(config), "inception");
	}

}