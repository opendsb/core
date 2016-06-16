package org.opendsb.messaging;

public class ReplyMessage extends DataMessage {

	private boolean successful;
	private String cause = "";

	public ReplyMessage(String destination, String origin, Object reply) {
		super(destination, origin, reply);
		this.successful = true;
		this.type = MessageType.REPLY;
	}

	public ReplyMessage(String destination, String origin, String cause) {
		super(destination, origin, null);
		this.cause = cause;
		this.successful = false;
		this.type = MessageType.REPLY;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public String getCause() {
		return cause;
	}
}