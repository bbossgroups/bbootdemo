package org.frameworkset.web.mcp;

import org.frameworkset.service.PreOrderTool;
import org.frameworkset.spi.InitializingBean;
import org.frameworkset.spi.ai.mcp.tools.server.MCPApiKeyServiceImpl;
import org.frameworkset.spi.ai.mcp.tools.server.MCPToolService;
import org.frameworkset.spi.ai.mcp.tools.server.MCPToolServiceImpl;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.annotations.RequestBody;
import org.frameworkset.util.annotations.RequestHeader;
import org.frameworkset.util.annotations.ResponseBody;
import reactor.core.publisher.Flux;

public class MCPServerController implements InitializingBean {
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MCPServerController.class);


	private MCPToolService mcpService;
    
    public @ResponseBody Flux<String> sse(@RequestHeader(name="Authorization") String authorizationHeader
    ) {
		
        String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
		return mcpService.sse(apiKey);
		 
        
    }
	
	public @ResponseBody Flux<String> sse_v1(String apiKey	) {
		
		return mcpService.sse(apiKey);
		
		
	}
	
	public @ResponseBody String message(@RequestHeader(name="Authorization") String authorizationHeader,String sessionId
										, @RequestBody String requestBody
	) {
		
		String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
		return mcpService.message(apiKey,sessionId,requestBody);	
		
		
	}
	
	public @ResponseBody Object streamable(@RequestHeader(name="Authorization") String authorizationHeader
			, @RequestBody String requestBody
	) {
		
		String apiKey = HttpRequestProxy.extractApiKeyFromBearer(authorizationHeader);
		return mcpService.streamable(apiKey,requestBody);
		
		
	}


    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        MCPApiKeyServiceImpl mcpApiKeyService = new MCPApiKeyServiceImpl();
        mcpApiKeyService.registMcpBeanTool("123456",new PreOrderTool());
        MCPToolServiceImpl mcpService = new MCPToolServiceImpl();
        mcpService.setMcpApiKeyService(mcpApiKeyService);
        this.mcpService = mcpService;
    }
}
