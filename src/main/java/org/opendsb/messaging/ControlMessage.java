package org.opendsb.messaging;

import java.util.HashMap;
import java.util.Map;

import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;

import com.google.gson.Gson;

public class ControlMessage extends BaseMessage {

	private ControlMessageType controlMessageType;

	private Map<String, String> controlInfo = new HashMap<>();

	private ControlMessage(String origin, ControlMessageType controlMessageType, Map<String, String> controlInfo) {
		super("control", origin);
		this.type = MessageType.CONTROL;
		this.controlMessageType = controlMessageType;
		this.controlInfo = controlInfo;
	}
	
	private ControlMessage(String origin, String destination, ControlMessageType controlMessageType, Map<String, String> controlInfo) {
		super(destination, origin);
		this.type = MessageType.CONTROL;
		this.controlMessageType = controlMessageType;
		this.controlInfo = controlInfo;
	}

	public ControlMessageType getControlMessageType() {
		return controlMessageType;
	}

	public boolean containsControlToken(String controlToken) {
		return controlInfo.containsKey(controlToken);
	}

	public String getControlInfo(String controlToken) throws IllegalArgumentException {
		if (controlInfo.containsKey(controlToken)) {
			return controlInfo.get(controlToken);
		} else {
			throw new IllegalArgumentException("Token '" + controlToken + "' not found within the message.");
		}
	}

	public static class Builder {
		
		private static Gson gson = new Gson();

		public ConnectionRequestMessageBuilder createConnectionRequestMessage(String transactionId, String origin) {
			return new ConnectionRequestMessageBuilder(transactionId, origin);
		}

		public ConnectionReplyMessageBuilder createConnectionReplyMessage(String transactionId, String origin) {
			return new ConnectionReplyMessageBuilder(transactionId, origin);
		}
		
		public UpdateRoutingTableCountBuilder createUpdateRouteCountMessage(String transactionId, String origin) {
			return new UpdateRoutingTableCountBuilder(transactionId, origin);
		}
		
		public CallAckMessageBuilder createCallAckMessage(String transactionId, String origin, String destination) {
			return new CallAckMessageBuilder(transactionId, origin, destination);
		}

		public abstract class ControlBuilder {
			
			protected ControlMessageType controlMessageType = ControlMessageType.CONNECTION_REQUEST;
			protected Map<String, String> controlInfo = new HashMap<>();
			private String origin;
			private String destination = null;

			private ControlBuilder(String transactionId, String origin) {
				this.origin = origin;
				controlInfo.put(ControlTokens.TRANSACTION_ID, transactionId);
			}
			
			private ControlBuilder(String transactionId, String origin, String destination) {
				this.origin = origin;
				this.destination = destination;
				controlInfo.put(ControlTokens.TRANSACTION_ID, transactionId);
			}
			
			public ControlBuilder addToken(String token, String value) {
				controlInfo.put(token, value);
				return this;
			}
			
			public ControlBuilder addRoutingTableCount(Map<String, Integer> routingTableCount) {
				addToken(ControlTokens.ROUTING_TABLE_COUNT, gson.toJson(routingTableCount));
				return this;
			}
			
			public ControlBuilder addClientId(String clientId) {
				addToken(ControlTokens.CLIENT_ID, clientId);
				return this;
			}
			
			public ControlBuilder addServerId(String serverId) {
				addToken(ControlTokens.SERVER_ID, serverId);
				return this;
			}

			public ControlMessage build() {
				if (destination != null) {
					return new ControlMessage(origin, destination, controlMessageType, controlInfo);
				} else {
					return new ControlMessage(origin, controlMessageType, controlInfo);
				}
			}
		}
		
		public class CallAckMessageBuilder extends ControlBuilder {
			public CallAckMessageBuilder(String transactionId, String origin, String destination) {
				super(transactionId, origin, destination);
				controlMessageType = ControlMessageType.CALL_ACK;
			}
		}

		public class ConnectionRequestMessageBuilder extends ControlBuilder {
			private ConnectionRequestMessageBuilder(String transactionId, String origin) {
				super(transactionId, origin);
				controlMessageType = ControlMessageType.CONNECTION_REQUEST;
			}
		}

		public class ConnectionReplyMessageBuilder extends ControlBuilder {
			private ConnectionReplyMessageBuilder(String transactionId, String origin) {
				super(transactionId, origin);
				controlMessageType = ControlMessageType.CONNECTION_REPLY;
			}
		}
		
		public class UpdateRoutingTableCountBuilder extends ControlBuilder {
			private UpdateRoutingTableCountBuilder(String transactionId, String origin) {
				super(transactionId, origin);
				controlMessageType = ControlMessageType.UPDATE_ROUTE_COUNT;
			}
		}
	}
}
