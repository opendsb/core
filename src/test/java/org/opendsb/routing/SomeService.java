package org.opendsb.routing;

import java.util.List;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.opendsb.client.BusClient;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;

public class SomeService implements Consumer<Message> {

	private static Logger logger = Logger.getLogger(SomeService.class);

	private BusClient client;

	private String name;

	private Subscription sub;

	public SomeService(Router localRouter, String name) {
		super();
		this.client = BusClient.of(localRouter);
		this.name = name;
	}

	public void registerMyself() {
		// Use the router to register the call in a topic
		sub = client.subscribe("Brasil/RJ/Clima/getTemperature", this);
	}

	public void cancelRegistration() {
		sub.cancel();
	}

	public String getName() {
		return name;
	}

	public Integer getTemperature(String neighbourhood) throws IllegalArgumentException {

		int temp = 25;

		logger.info("Checking weather for the neighbourhood '" + neighbourhood + "' ...");

		switch (neighbourhood) {
		case "Fundao": {
			temp = 35;
			break;
		}

		case "Botafogo": {
			temp = 22;
			break;
		}

		case "Bangu": {
			temp = 50;
			break;
		}

		default: {
			throw new IllegalArgumentException("No infromation about '" + neighbourhood + "'.");
		}
		}

		return temp;
	}

	@Override
	public void accept(Message t) {
		// Make sure the message is a Call
		// Unwrap parameters
		// execute call
		// prepare reply message regardless of success / failure
		// Send message to the pre arranged topic

		if (t instanceof CallMessage) {

			CallMessage msg = (CallMessage) t;

			String neighbourhood = "";

			try {
				List<Object> params = msg.getParameters();
				if (params.size() == 1) {
					logger.info("Calling method");
					neighbourhood = (String) params.get(0);
					Integer ans = getTemperature(neighbourhood);
					logger.info("Replying '" + ans + "' to '" + msg.getReplyTo() + "'");
					client.publishReply(msg.getReplyTo(), ans);
				} else {
					throw new IllegalArgumentException(
							"Invalid number of arguments");
				}
			} catch (IllegalArgumentException e) {
				client.postFailureReply(msg.getReplyTo(), e.getMessage());
			}

		} else {
			// Error
		}
	}

}
