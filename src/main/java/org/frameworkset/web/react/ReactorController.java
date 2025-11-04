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
import org.frameworkset.spi.ai.model.AudioEvent;
import org.frameworkset.spi.ai.model.ImageEvent;
import org.frameworkset.spi.ai.model.ServerEvent;
import org.frameworkset.spi.ai.util.AudioDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.annotations.RequestBody;
import org.frameworkset.util.annotations.ResponseBody;
import org.frameworkset.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
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
        requestMap.put("max_tokens", 8192);
        requestMap.put("temperature", 0.7);
        return HttpRequestProxy.streamChatCompletion("deepseek","/chat/completions",requestMap);
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
        requestMap.put("max_tokens", 8192);
        requestMap.put("temperature", 0.7);
        return HttpRequestProxy.streamChatCompletion("deepseek","/chat/completions",requestMap).limitRate(5) // 限制请求速率
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
        requestMap.put("max_tokens", 8192);
        requestMap.put("temperature", 0.7);
        Flux<ServerEvent> flux = HttpRequestProxy.streamChatCompletionEvent("deepseek","/chat/completions",requestMap);

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
        requestMap.put("max_tokens", 8192);
        requestMap.put("temperature", 0.7);
        Flux<ServerEvent> flux = HttpRequestProxy.streamChatCompletionEvent("deepseek","/chat/completions",requestMap);

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
     * 智能问答功能，背压案例 - 带会话记忆功能（完善版）
     * http://127.0.0.1/demoproject/chatBackuppressSession.html
     * @param questions
     * @return
     */
    public Flux<List<ServerEvent>> deepseekBackuppressSession(@RequestBody Map<String,Object> questions) {

        String selectedModel = (String)questions.get("selectedModel");
        Boolean reset = (Boolean) questions.get("reset");
        Boolean deepThink = (Boolean) questions.get("deepThink");

        if(reset != null && reset){
            sessionMemory.clear();
        }
        String message = (String)questions.get("message");
        Map<String, Object> requestMap = new HashMap<>();
        if(selectedModel.equals("deepseek")) {
            if(deepThink == null || !deepThink) {
                requestMap.put("model", "deepseek-chat");
            }
            else {
                requestMap.put("model", "deepseek-reasoner");
            }
            
        }
        else {
            requestMap.put("model", "Qwen/Qwen3-Next-80B-A3B-Instruct");//指定模型
        }
    
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = new ArrayList<>(sessionMemory);
        
        // 添加当前用户消息
        Map<String, Object> userMessage = MessageBuilder.buildUserMessage( message);
        sessionMemory.add(userMessage);
        messages.add(userMessage);
    
        requestMap.put("messages", messages);
        requestMap.put("stream", true);
        requestMap.put("max_tokens", 8192);
        requestMap.put("temperature", 0.7);
        Flux<ServerEvent> flux = HttpRequestProxy.streamChatCompletionEvent(selectedModel,"/chat/completions",requestMap);
    
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
                //答案前后都可以添加链接和标题
                if(event.isFirst() || event.isDone()){
                    event.addExtendData("url","https://www.bbossgroups.com");
                    event.addExtendData("title","bboss官网");
                }
                if(!event.isDone() ) {
                    // 累积回答内容
                    if(event.getData() != null) {
                        completeAnswer.append(event.getData());
                    }
                } else  {
                    
                    if( completeAnswer.length() > 0) {
                        // 当收到完成信号且有累积内容时，将完整回答添加到会话记忆
                        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(completeAnswer.toString());                        
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
     * 图片识别功能，带会话记忆功能
     * http://127.0.0.1/demoproject/chatBackuppressSession.html
     * @param questions
     * @return
     */
    public Flux<List<ServerEvent>>  qwenvl(@RequestBody Map<String,Object> questions) throws InterruptedException {
        String selectedModel = (String)questions.get("selectedModel");
        Boolean reset = (Boolean) questions.get("reset");
        if(reset != null && reset){
            sessionMemory.clear();
        }
        Boolean deepThink = (Boolean) questions.get("deepThink");
        String message  = null;
        message = (String)questions.get("message");
        if(SimpleStringUtil.isEmpty( message)){
            message = "介绍图片内容并计算结果";
        }
        Map<String, Object> requestMap = new HashMap<>();
        if(selectedModel.equals("qwenvlplus")) {
            requestMap.put("model", "qwen3-vl-plus");
        }
        else{

            requestMap.put("model", "Qwen/Qwen3-VL-32B-Thinking");
        }
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = new ArrayList<>(sessionMemory);

        String imageBase64  = (String)questions.get("imageBase64");
        String imageUrl = (String)questions.get("imageUrl");
        if(imageUrl != null) {
            imageUrl = imageUrl.trim();
        }
        

        List<String> imageUrls = new ArrayList<>();
        if(SimpleStringUtil.isNotEmpty(imageUrl)) {
            imageUrls.add(imageUrl);
        }
        if(SimpleStringUtil.isNotEmpty(imageBase64)) {
            imageUrls.add(imageBase64);
        }
 
        Map<String, Object> userMessage =MessageBuilder.buildInputImagesMessage(message,imageUrls.toArray(new String[]{}));
        messages.add(userMessage);



        requestMap.put("messages", messages);
        requestMap.put("stream", true);

        // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
        Map extra_body = new LinkedHashMap();
        if(deepThink == null)
            deepThink = true;
        extra_body.put("enable_thinking",deepThink);
        extra_body.put("thinking_budget",81920);
        requestMap.put("extra_body",extra_body);

 
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
        Flux<List<ServerEvent>> flux = null;
        if(selectedModel.equals("qwenvlplus")){
            flux = HttpRequestProxy.streamChatCompletionEvent("qwenvlplus","/compatible-mode/v1/chat/completions",requestMap).limitRate(5) // 限制请求速率
                .buffer(3);
        }
        else{
            flux = HttpRequestProxy.streamChatCompletionEvent("guiji","/chat/completions",requestMap).limitRate(5) // 限制请求速率
                    .buffer(3);
        }
        flux = flux.doOnNext(bufferedEvents -> {
                    // 处理模型响应并更新会话记忆
                    for(ServerEvent event : bufferedEvents) {
                        //答案前后都可以添加链接和标题
                        if(event.isFirst() || event.isDone()){
                            event.addExtendData("url","https://www.bbossgroups.com");
                            event.addExtendData("title","bboss官网");
                        }
                        event.getContentType();
                        if(!event.isDone() ) {
                             
                            // 累积回答内容
                            if(event.getData() != null) {
                                completeAnswer.append(event.getData());
                            }
                        } else  {
                            
                            if( completeAnswer.length() > 0) {
                                // 当收到完成信号且有累积内容时，将完整回答添加到会话记忆
                                Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(completeAnswer.toString());
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
     * 图片生成功能，无会话记忆功能
     * http://127.0.0.1/demoproject/chatBackuppressSession.html
     * 参考文档：
     * https://bailian.console.aliyun.com/?spm=5176.29597918.J_SEsSjsNv72yRuRFS2VknO.2.74ba7b08ig5jxD&tab=api#/api/?type=model&url=2975126
     * '{
     *     "model": "qwen-image-plus",
     *     "input": {
     *         "messages": [
     *             {
     *                 "role": "user",
     *                 "content": [
     *                     {
     *                         "text": "一副典雅庄重的对联悬挂于厅堂之中，房间是个安静古典的中式布置，桌子上放着一些青花瓷，对联上左书“义本生知人机同道善思新”，右书“通云赋智乾坤启数高志远”， 横批“智启通义”，字体飘逸，中间挂在一着一副中国风的画作，内容是岳阳楼。"
     *                     }
     *                 ]
     *             }
     *         ]
     *     },
     *     "parameters": {
     *         "negative_prompt": "",
     *         "prompt_extend": true,
     *         "watermark": true,
     *         "size": "1328*1328"
     *     }
     * @param questions
     * @return
     */
    public @ResponseBody Map genImageByqwenimage(@RequestBody Map<String,Object> questions) throws InterruptedException {
//        String selectedModel = (String)questions.get("selectedModel");
    
        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "生成一颗桂花树";
        }
 


        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "qwen-image-plus");


        Map<String,Object> input = new LinkedHashMap<>();
        
       
        // 构建消息历史列表，包含之前的会话记忆
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = MessageBuilder.buildGenImageMessage(message);
        messages.add(userMessage);
        input.put("messages", messages);
        requestMap.put("input", input);

        // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
        Map parameters = new LinkedHashMap();
        parameters.put("negative_prompt","");
        parameters.put("prompt_extend",true);
        parameters.put("watermark",false);
        parameters.put("size","1328*1328");
        requestMap.put("parameters", parameters);

        ImageEvent data = HttpRequestProxy.multimodalImageGeneration("qwenvlplus","/api/v1/services/aigc/multimodal-generation/generation",requestMap);
        
        String imageUrl = data.getImageUrl();
        
        Map ret = new HashMap();
        ret.put("imageUrl",imageUrl);
        return ret;
    }
 
	/**
     * 音频识别功能
     * https://bailian.console.aliyun.com/?spm=5176.29597918.J_SEsSjsNv72yRuRFS2VknO.2.74ba7b08ig5jxD&tab=doc#/doc/?type=model&url=2979031
     * @param audio 音频文件
     * @param request HTTP请求
     * @return
     */
    public Flux<List<ServerEvent>> audioFileRecognition(MultipartFile audio, HttpServletRequest request) {
        String selectedModel = request.getParameter("selectedModel");
        String reset = request.getParameter("reset");
        if (reset != null && reset.equals("true")) {
            sessionMemory.clear();
        }
        String deepThink_ = request.getParameter("deepThink");
        String message = null;
        message = request.getParameter("message");
        if (SimpleStringUtil.isEmpty(message)) {
            message = "介绍音频内容";
        }
    
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "qwen3-asr-flash");
    
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = new ArrayList<>(sessionMemory);
    
        // 添加当前用户消息
        Map<String, Object> userMessage = MessageBuilder.buildAudioSystemMessage( message);
        messages.add(userMessage);
    
        //直接设置音频url地址
//        MessageBuilder.buildAudioMessage("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3");
        //将音频文件转换为base64编码
        userMessage = MessageBuilder.buildAudioMessage(new AudioDataBuilder() {
            @Override
            public String buildAudioBase64Data() {
                String base64Audio = null;
                if (audio != null && !audio.isEmpty()) {
                    try {
                        byte[] audioBytes = audio.getBytes();
                        base64Audio = "data:" + audio.getContentType() + ";base64," +
                                java.util.Base64.getEncoder().encodeToString(audioBytes);
                    } catch (Exception e) {
                        logger.error("音频文件转换base64失败", e);
                    }
                }
                return base64Audio;
            }
        });

//        sessionMemory.add(userMessage);
        messages.add(userMessage);
        
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", messages);
        requestMap.put("input", input);
    
        Map asr_options = new LinkedHashMap();
        asr_options.put("enable_itn", true);
        Map parameters = new LinkedHashMap();
        parameters.put("asr_options", asr_options);
        parameters.put("incremental_output", true);
        requestMap.put("parameters", parameters);
        requestMap.put("result_format", "message");
        
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
        Flux<List<ServerEvent>> flux = HttpRequestProxy.streamChatCompletionEvent("qwenvlplus", 
                "/api/v1/services/aigc/multimodal-generation/generation", requestMap)
                .doOnNext(chunk -> {
                    if (!chunk.isDone()) {
                        logger.info(chunk.getData());
                    }
                }).limitRate(5) // 限制请求速率
                .buffer(3);
    
        flux = flux.doOnNext(bufferedEvents -> {
            // 处理模型响应并更新会话记忆
            for (ServerEvent event : bufferedEvents) {
                // 答案前后都可以添加链接和标题
                if (event.isFirst() || event.isDone()) {
                    event.addExtendData("url", "https://www.bbossgroups.com");
                    event.addExtendData("title", "bboss官网");
                }
                event.getContentType();
                if (!event.isDone()) {
                    // 累积回答内容
                    if (event.getData() != null) {
                        completeAnswer.append(event.getData());
                    }
                } else {
                    if (completeAnswer.length() > 0) {
                        // 当收到完成信号且有累积内容时，将完整回答添加到会话记忆
                        Map<String, Object> assistantMessage = MessageBuilder.buildAssistantMessage(completeAnswer.toString());
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
     * 音频生成服务
     http://127.0.0.1/demoproject/reactor/genAudioByqwentts.api
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody AudioEvent genAudioByqwentts(@RequestBody Map<String,Object> questions) throws InterruptedException {
//        String selectedModel = (String)questions.get("selectedModel");

        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "诗词朗诵：窗前明月光；疑似地上霜；举头望明月；低头思故乡。";
        }



        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "qwen3-tts-flash");




        Map<String,Object> inputVoice = new LinkedHashMap();
        inputVoice.put("text",message);
        inputVoice.put("voice","Cherry");
        inputVoice.put("language_type","Chinese");

        requestMap.put("input",inputVoice);
        AudioEvent audioEvent = HttpRequestProxy.multimodalAudioGeneration("qwenvlplus","/api/v1/services/aigc/multimodal-generation/generation",requestMap);



//
        return audioEvent;
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
