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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author biaoping.yin
 * @Date 2025/9/29
 */

public class AsyncStreamServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // 启动异步上下文
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(30000); // 设置超时时间
        
        // 设置响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        
        // 获取输出流
        ServletOutputStream outputStream = response.getOutputStream();
        
        // 创建 Flux 数据流
        Flux<String> flux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> {
                    String data = "data: "+ i +"," + LocalDateTime.now() + "\n\n";
                    System.out.println(data);
                    return data;
                })
                .take(10); // 限制发送10条数据
        
        // 订阅并写入数据
        flux.subscribe(
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
    }
}

