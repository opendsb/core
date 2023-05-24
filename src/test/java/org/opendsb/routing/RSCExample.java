package org.opendsb.routing;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.messaging.ReplyMessage;

public class RSCExample {

	private static Logger logger = Logger.getLogger(RSCExample.class);


	@BeforeClass
	public static void setup() {
	}

	@Test
	public void testSuccessfulRSC() throws Exception {

		Router router = Router.newRouter();
		BusClient client = BusClient.of(router);

		SomeService service = new SomeService(router, "weatherService");
		service.registerMyself();
		
		Object[] parametersArr = { "Fundao" };

		List<Object> parameters = Arrays.asList(parametersArr);

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

		Router router = Router.newRouter();
		BusClient client = BusClient.of(router);

		SomeService service = new SomeService(router, "weatherService");
		service.registerMyself();

		String[] parametersArr = { "Onde Judas perdeu as botas" };

		List<Object> parameters = Arrays.asList((Object[])parametersArr);

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
