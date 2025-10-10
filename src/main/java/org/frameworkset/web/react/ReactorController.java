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

import org.frameworkset.spi.InitializingBean;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2025/9/30
 */
public class ReactorController implements InitializingBean {
    private Logger logger = LoggerFactory.getLogger(ReactorController.class);
    public Flux<String> chatTest() {

//		NettyWebServer nettyWebServer = new NettyWebServer();
        logger.info("");

        Flux<String> flux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> {
                    String data = "中文data: "+ i +"," + LocalDateTime.now() + "\n\n";
                    logger.info(data);
                    return data;
                })
                .take(10); // 限制发送10条数据
        return flux;
        //chat交互测试
//        return Flux.interval(Duration.ofSeconds(1)).take(10).map(sequence -> "{" + "    \"data\": \"33\"," + "    \"count\": \"" + sequence + "\"" + "}");
    }


    public Flux<String> deepseekChat(String message) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);

        requestMap.put("messages", messages);
        requestMap.put("stream", true);
        requestMap.put("max_tokens", 2048);
        requestMap.put("temperature", 0.7);
        return HttpRequestProxy.streamChatCompletion("/chat/completions",requestMap);
//                .doOnSubscribe(subscription -> logger.info("开始订阅流..."))
//                .doOnNext(chunk -> System.out.print(chunk))
//                .doOnComplete(() -> logger.info("\n=== 流完成 ==="))
//                .doOnError(error -> logger.error("错误: " + error.getMessage(),error));
//                .subscribe();

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
        //加载配置文件，启动负载均衡器
        HttpRequestProxy.startHttpPools("application.properties");
    }
}
