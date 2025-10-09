package org.frameworkset.web.react;
/**
 * Copyright 2025 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import reactor.core.publisher.Flux;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author biaoping.yin
 * @Date 2025/9/29
 */

public class DynamicAsyncServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 检测是否为 SSE 请求
        String acceptHeader = request.getHeader("Accept");
        boolean isSSE = "text/event-stream".equals(request.getContentType()) || 
                       (acceptHeader != null && acceptHeader.contains("text/event-stream"));
        
        AsyncContext asyncContext;
        if (isSSE) {
            // 动态启动异步上下文
            if (request.isAsyncSupported()) {
                asyncContext = request.startAsync();
                asyncContext.setTimeout(30000);
            } else {
                throw new ServletException("Async not supported");
            }
            handleStreamResponse(asyncContext);
        } else {
////             普通同步处理
//            asyncContext = request.startAsync();
//            asyncContext.setAsyncMode(AsyncContext.ASYNC_MODE_BLOCKING);
        }
        
       
    }
    
    private void handleStreamResponse(AsyncContext asyncContext) {
        HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
        
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        try {
            OutputStream outputStream = response.getOutputStream(); // Changed from ServletOutputStream to OutputStream
            
            // 创建数据流
            Flux.interval(Duration.ofSeconds(1))
                .take(10)
                .map(i -> "data: " + System.currentTimeMillis() + "\n\n")
                .subscribe(
                    data -> {
                        try {
                            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                        } catch (IOException e) {
                            asyncContext.complete();
                        }
                    },
                    error -> asyncContext.complete(),
                    () -> asyncContext.complete()
                );
                
        } catch (IOException e) {
            asyncContext.complete();
        }
    }
}

