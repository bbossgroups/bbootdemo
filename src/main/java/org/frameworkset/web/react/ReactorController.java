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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.InitializingBean;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.spi.remote.http.reactor.ServerEvent;
import org.frameworkset.util.annotations.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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


    /**
     * http://127.0.0.1/demoproject/chatpost.html
     * @param questions
     * @return
     */
    public Flux<String> deepseekChat(@RequestBody Map<String,Object> questions) {
        String message = (String)questions.get("message");
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
     * http://127.0.0.1/demoproject/chatpostBackpress.html
     * @param questions
     * @return
     */
    public Flux<List<String>> deepseekChatBackpress(@RequestBody Map<String,Object> questions) {
        String message = (String)questions.get("message");
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
        return HttpRequestProxy.streamChatCompletion("/chat/completions",requestMap).limitRate(5) // 限制请求速率
                .buffer(3) ;   // 每3个元素缓冲一次
//                .doOnSubscribe(subscription -> logger.info("开始订阅流..."))
//                .doOnNext(chunk -> System.out.print(chunk))
//                .doOnComplete(() -> logger.info("\n=== 流完成 ==="))
//                .doOnError(error -> logger.error("错误: " + error.getMessage(),error));
//                .subscribe();

    }


    /**
     * http://127.0.0.1/demoproject/chatpostServerEvent.html
     * @param questions
     * @return
     */
    public Flux<ServerEvent> deepseekChatServerEvent(@RequestBody Map<String,Object> questions) {
        String message = (String)questions.get("message");
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
        Flux<ServerEvent> flux = HttpRequestProxy.streamChatCompletionEvent("/chat/completions",requestMap);

        return flux.doOnNext(chunk -> {
            if(!chunk.isDone())
                logger.info(chunk.getData());
        });
       

    }


    /**
     * 背压案例
     * http://127.0.0.1/demoproject/chatBackuppress.html
     * @param questions
     * @return
     */
    public Flux<List<ServerEvent>> deepseekChatServerEventBackuppress(@RequestBody Map<String,Object> questions) {
        String message = (String)questions.get("message");
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
        Flux<ServerEvent> flux = HttpRequestProxy.streamChatCompletionEvent("/chat/completions",requestMap);

        return flux.doOnNext(chunk -> {
            if(!chunk.isDone()) {
                logger.info(chunk.getData());
            }
        }).limitRate(5) // 限制请求速率
                .buffer(3) ;   // 每3个元素缓冲一次;


    }
    // 使用静态变量存储会话记忆（实际项目中建议使用缓存或数据库）
    static List<Map<String, Object>> sessionMemory = new ArrayList<>();

     /**
     * 背压案例 - 带会话记忆功能（完善版）
     * http://127.0.0.1/demoproject/chatBackuppressSession.html
     * @param questions
     * @return
     */
    public Flux<List<ServerEvent>> deepseekBackuppressSession(@RequestBody Map<String,Object> questions) {

        Boolean reset = (Boolean) questions.get("reset");
        if(reset != null && reset){
            sessionMemory.clear();
        }
        String message = (String)questions.get("message");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "deepseek-chat");
    
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = new ArrayList<>(sessionMemory);
        
        // 添加当前用户消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);
    
        requestMap.put("messages", messages);
        requestMap.put("stream", true);
        requestMap.put("max_tokens", 2048);
        requestMap.put("temperature", 0.7);
        Flux<ServerEvent> flux = HttpRequestProxy.streamChatCompletionEvent("/chat/completions",requestMap);
    
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
    
        return flux.doOnNext(chunk -> {
            if(!chunk.isDone()) {
                logger.info(chunk.getData());
            }
        })
        .limitRate(5) // 限制请求速率
        .buffer(3) // 每3个元素缓冲一次
        .doOnNext(bufferedEvents -> {
            // 处理模型响应并更新会话记忆
            for(ServerEvent event : bufferedEvents) {
                if(!event.isDone() ) {
                    // 累积回答内容
                    if(event.getData() != null) {
                        completeAnswer.append(event.getData());
                    }
                } else  {
                    event.addExtendData("url","https://www.bbossgroups.com");
                    event.addExtendData("title","bboss官网");
                    if( completeAnswer.length() > 0) {
                        // 当收到完成信号且有累积内容时，将完整回答添加到会话记忆
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", completeAnswer.toString());
                        sessionMemory.add(assistantMessage);

                        // 维护记忆窗口大小为20
                        if (sessionMemory.size() > 20) {
                            sessionMemory.remove(0);
                        }
                    }
                    
                    
                }
            }
        });
    }

    /**
     * 带会话记忆功能
     * http://127.0.0.1/demoproject/chatBackuppressSession.html
     * @param questions
     * @return
     */
    public Flux<List<ServerEvent>>  qwenvl(@RequestBody Map<String,Object> questions) throws InterruptedException {
        Boolean reset = (Boolean) questions.get("reset");
        if(reset != null && reset){
            sessionMemory.clear();
        }
        String message  = null;
        message = (String)questions.get("message");
        if(SimpleStringUtil.isEmpty( message)){
            message = "介绍图片内容并计算结果";
        }
//		
//		[
//		{
//			"type": "image_url",
//				"image_url": {
//			"url": "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg"
//		},
//		},
//		{"type": "text", "text": "这道题怎么解答？"},
//            ]
        
        List content = new ArrayList<>();
        Map contentData = new LinkedHashMap();
        contentData.put("type", "image_url");
        contentData.put("image_url", new HashMap<String, String>(){{
            String imageUrl = (String)questions.get("imageUrl");
            if(imageUrl != null) {
                imageUrl = imageUrl.trim();
            }
            if(SimpleStringUtil.isEmpty(imageUrl)){
                imageUrl = "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg";
            }
            put("url", imageUrl);
        }});
        content.add(contentData);
//		content.add(new HashMap<String, Object>(){{
//			put("type", "image_url");
//			put("image_url", new HashMap<String, String>(){{
//				put("url", "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg");
//			}});
//		}});
        contentData = new LinkedHashMap();
        contentData.put("type", "text");
        contentData.put("text", message);;
        content.add(contentData);


        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "qwen3-vl-plus");
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = new ArrayList<>(sessionMemory);
//        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);
        messages.add(userMessage);



        requestMap.put("messages", messages);
        requestMap.put("stream", true);

        // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
        Map extra_body = new LinkedHashMap();
        extra_body.put("enable_thinking",true);
        extra_body.put("thinking_budget",81920);
        requestMap.put("extra_body",extra_body);

//		{
//				'enable_thinking': True,
//				"thinking_budget": 81920},
//		requestMap.put("max_tokens", 2048);
//		requestMap.put("temperature", 0.7);
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
        Flux<List<ServerEvent>> flux = HttpRequestProxy.streamChatCompletionEvent("qwenvlplus","/compatible-mode/v1/chat/completions",requestMap).limitRate(5) // 限制请求速率
                .buffer(3).doOnNext(bufferedEvents -> {
                    // 处理模型响应并更新会话记忆
                    for(ServerEvent event : bufferedEvents) {
                        if(!event.isDone() ) {
                            // 累积回答内容
                            if(event.getData() != null) {
                                completeAnswer.append(event.getData());
                            }
                        } else  {
                            event.addExtendData("url","https://www.bbossgroups.com");
                            event.addExtendData("title","bboss官网");
                            if( completeAnswer.length() > 0) {
                                // 当收到完成信号且有累积内容时，将完整回答添加到会话记忆
                                Map<String, Object> assistantMessage = new HashMap<>();
                                assistantMessage.put("role", "assistant");
                                assistantMessage.put("content", completeAnswer.toString());
                                sessionMemory.add(assistantMessage);

                                // 维护记忆窗口大小为20
                                if (sessionMemory.size() > 20) {
                                    sessionMemory.remove(0);
                                }
                            }


                        }
                    }
                });
        
        return flux;
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
