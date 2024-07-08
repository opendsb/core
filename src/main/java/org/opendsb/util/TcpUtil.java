package org.opendsb.util;

import javax.websocket.Session;
import java.net.InetSocketAddress;

public class TcpUtil {

    public static String getClientIpAddress(Session session) {
        // Obtém o endereço remoto da conexão WebSocket
        InetSocketAddress remoteAddress = (InetSocketAddress) session.getUserProperties().get("javax.websocket.endpoint.remoteAddress");

        if (remoteAddress != null) {
            String clientIp = remoteAddress.getAddress().getHostAddress();
            // System.out.println("Mensagem recebida de: " + clientIp);
            return clientIp;
        }

        return "remoteAddress is null !";
    }
}

