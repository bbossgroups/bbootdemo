package org.frameworkset.web.socket;

import org.frameworkset.web.socket.inf.*;
import org.slf4j.Logger;

/**
 *
 * @author dayu
 */
public class SystemWebSocketHandler implements WebSocketHandler {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(SystemWebSocketHandler.class);
	private boolean supportsPartialMessages;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("connect to the websocket success......");
        session.sendMessage(new TextMessage("Server:connected OK!"));
    }

    @Override
    public void handleMessage(WebSocketSession wss, WebSocketMessage<?> wsm) throws Exception {
        TextMessage returnMessage = new TextMessage(wsm.getPayload()
				+ " received at server");
		wss.sendMessage(returnMessage);
    }

    @Override
    public void handleTransportError(WebSocketSession wss, Throwable thrwbl) throws Exception {
        if(wss.isOpen()){
            wss.close();
        }
       logger.info("websocket connection closed......");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wss, CloseStatus cs) throws Exception {
        logger.info("websocket connection closed......");
    }

    @Override
    public boolean supportsPartialMessages() {
        return supportsPartialMessages;
    }
    
}
