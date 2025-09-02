package org.frameworkset.web.socket;

import org.frameworkset.http.ServerHttpRequest;
import org.frameworkset.http.ServerHttpResponse;
import org.frameworkset.web.socket.handler.HttpSessionHandshakeInterceptor;
import org.frameworkset.web.socket.inf.WebSocketHandler;
import org.slf4j.Logger;

import java.util.Map;



public class HandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(HandshakeInterceptor.class);
	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {
        logger.info("Before Handshake");
		return super.beforeHandshake(request, response, wsHandler, attributes);
	}

	@Override
	public void afterHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception ex) {
		logger.info("After Handshake");
		super.afterHandshake(request, response, wsHandler, ex);
	}

}
