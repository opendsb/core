package org.opendsb.routing;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.messaging.Subscription;

public class BasicRoutingTest {

	private static Logger logger = Logger.getLogger(BasicRoutingTest.class);

	private static String log4JFile = "conf/log4j.properties";

	@BeforeClass
	public static void setup() {
		PropertyConfigurator.configureAndWatch(log4JFile);
	}

	@Test
	public void testSimpleRouting() throws Exception {

		Router router = Router.newRouter();

		BusClient client = BusClient.of(router);

		logger.info("Starting routing test ...");

		String id = "Local";
		Subscription sub1 = client.subscribe(id,
				m -> System.out.println("Message received\n  Receptor 'Local'\n  messageId'" + m.getMessageId() + "'"));
		id = "Brasil";
		Subscription sub2 = client.subscribe(id, m -> System.out
				.println("Message received\n  Receptor 'Brasil'\n  messageId'" + m.getMessageId() + "'"));
		id = "Argentina";
		Subscription sub3 = client.subscribe(id, m -> System.out
				.println("Message received\n  Receptor 'Argentina'\n  messageId'" + m.getMessageId() + "'"));
		id = "Brasil/RJ";
		Subscription sub4 = client.subscribe(id,
				m -> System.out.println("Message received\n  Receptor 'RJ'\n  messageId'" + m.getMessageId() + "'"));
		id = "SP";
		Subscription sub5 = client.subscribe(id,
				m -> System.out.println("Message received\n  Receptor 'SP'\n  messageId'" + m.getMessageId() + "'"));

		logger.info("Routing message ...");

		client.publishData("Brasil/RJ", "SomeStuff");

		logger.info("Tracking nodes");

		logger.info("Routing message...");

		client.publishData("Brasil/RJ/Clima", "SomeOtherStuff");

		logger.info("Test has come to a finish.");

		Thread.sleep(100);

		sub1.cancel();
		sub2.cancel();
		sub3.cancel();
		sub4.cancel();
		sub5.cancel();
	}

	// Subscription tests
	@Test
	public void testSubscriptions() throws Exception {

		logger.info("Subscription tests Begin -----------------------------------------------------------------------");

		Router router = Router.newRouter();

		BusClient client = BusClient.of(router);
		
		String topic = "Brasil/BA";
		Subscription sub1 = client.subscribe(topic, m -> System.out
				.println("Message received\n  Receptor '" + topic + "'\n  messageId'" + m.getMessageId() + "'"));

		String topic2 = "Brasil/RJ/Politica";
		Subscription sub2 = client.subscribe(topic2, m -> System.out
				.println("Message received\n  Receptor '" + topic2 + "'\n  messageId'" + m.getMessageId() + "'"));

		client.publishData("Brasil/BA/Clima", "More stuff.");

		client.publishData("Brasil/RJ/Politica", "Even more stuff");

		Thread.sleep(100);

		sub1.cancel();
		sub2.cancel();

		logger.info("Subscription tests End -----------------------------------------------------------------------");
	}
}
