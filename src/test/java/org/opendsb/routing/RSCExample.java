package org.opendsb.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.client.DefaultBusClient;
import org.opendsb.messaging.ReplyMessage;

public class RSCExample {

	private static Logger logger = Logger.getLogger(RSCExample.class);

	private static String log4JFile = "conf/log4j.properties";

	@BeforeClass
	public static void setup() {
		PropertyConfigurator.configureAndWatch(log4JFile);
	}

	@Test
	public void testSuccessfulRSC() throws Exception {

		Router router = new LocalRouter();
		BusClient client = DefaultBusClient.of(router);

		SomeService service = new SomeService(router, "weatherService");
		service.registerMyself();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("neighbourhood", "Fundao");

		String serviceName = "Brasil/RJ/Clima/getTemperature";

		logger.info("Calling service '" + serviceName + "'");

		Future<ReplyMessage> reply = client.call(serviceName, parameters);

		logger.info("Waiting response from '" + serviceName + "'");

		ReplyMessage m = reply.get();
		StringBuilder builder = new StringBuilder();
		builder.append("Resposta recebida:\n  Address: '" + m.getDestination() + "'\n  messageId: '" + m.getMessageId()
				+ "'\n");
		if (m.isSuccessful()) {
			builder.append("  Anwser: '" + m.getData() + "'\n");
		} else {
			builder.append("  Failure Reason: '" + m.getCause() + "'\n");
		}
		logger.info(builder.toString());
	}

	@Test
	public void testFailedRSC() throws Exception {

		Router router = new LocalRouter();
		BusClient client = DefaultBusClient.of(router);

		SomeService service = new SomeService(router, "weatherService");
		service.registerMyself();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("neighbourhood", "Onde Judas perdeu as botas");

		Future<ReplyMessage> reply = client.call("Brasil/RJ/Clima/getTemperature", parameters);

		ReplyMessage m = reply.get();
		StringBuilder builder = new StringBuilder();
		builder.append("Resposta recebida:\n  Address: '" + m.getDestination() + "'\n  messageId: '" + m.getMessageId()
				+ "'\n");
		if (m.isSuccessful()) {
			builder.append("  Anwser: '" + m.getData() + "'\n");
		} else {
			builder.append("  Failure Reason: '" + m.getCause() + "'\n");
		}
		logger.info(builder.toString());
	}

}
