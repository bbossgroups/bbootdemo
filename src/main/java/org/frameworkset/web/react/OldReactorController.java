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
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.material.ReponseStoreFilePathFunction;
import org.frameworkset.spi.ai.material.StoreFilePathFunction;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIAgentUtil;
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
public class OldReactorController implements InitializingBean {
    private Logger logger = LoggerFactory.getLogger(OldReactorController.class);
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
        return AIAgentUtil.streamChatCompletion("deepseek","/chat/completions",requestMap);
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
        return AIAgentUtil.streamChatCompletion("deepseek","/chat/completions",requestMap).limitRate(5) // 限制请求速率
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
        Flux<ServerEvent> flux = AIAgentUtil.streamChatCompletionEvent("deepseek","/chat/completions",requestMap);

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
        Flux<ServerEvent> flux = AIAgentUtil.streamChatCompletionEvent("deepseek","/chat/completions",requestMap);

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
            selectedModel = "qwenvlplus";
//            requestMap.put("model", "Qwen/Qwen3-Next-80B-A3B-Instruct");//指定硅基模型
//            requestMap.put("model", "qwen-plus");//指定百炼千问模型
            requestMap.put("model", "qwen3-max");//指定百炼千问模型

            
            
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
        String completionsUrl = selectedModel.equals("deepseek")? "/chat/completions":"/compatible-mode/v1/chat/completions";
        Flux<ServerEvent> flux = AIAgentUtil.streamChatCompletionEvent(selectedModel,completionsUrl,requestMap);
    
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
        if(selectedModel.equals("qwenvlplus")) {//阿里百炼模型服务
            requestMap.put("model", "qwen3-vl-plus");
        }
        else{ //硅基流动图片识别服务

            requestMap.put("model", "Qwen/Qwen3-VL-32B-Thinking");
        }
        // 构建消息历史列表，包含之前的会话记忆
        List<Map<String, Object>> messages = new ArrayList<>(sessionMemory);

        List<String> imagesBase64  = (List)questions.get("imagesBase64");
        String imageUrl = (String)questions.get("imageUrl");
        if(imageUrl != null) {
            imageUrl = imageUrl.trim();
        }
        

        List<String> imageUrls = new ArrayList<>();
        if(SimpleStringUtil.isNotEmpty(imageUrl)) {
            imageUrls.add(imageUrl);
        }
        if(SimpleStringUtil.isNotEmpty(imagesBase64)) {
            for(String tmp:imagesBase64)
            imageUrls.add(tmp);
        }
 
        Map<String, Object> userMessage = MessageBuilder.buildInputImagesMessage(message,imageUrls.toArray(new String[]{}));
        messages.add(userMessage);



        requestMap.put("messages", messages);
        requestMap.put("stream", true);

        // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
        if(deepThink == null)
            deepThink = true;
        requestMap.put("enable_thinking",deepThink);
        requestMap.put("thinking_budget",81920);

 
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
        Flux<List<ServerEvent>> flux = null;
        if(selectedModel.equals("qwenvlplus")){
            flux = AIAgentUtil.streamChatCompletionEvent("qwenvlplus","/compatible-mode/v1/chat/completions",requestMap).limitRate(5) // 限制请求速率
                .buffer(3);
        }
        else{
            flux = AIAgentUtil.streamChatCompletionEvent("guiji","/chat/completions",requestMap).limitRate(5) // 限制请求速率
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

        ImageEvent data = AIAgentUtil.multimodalImageGeneration("qwenvlplus","/api/v1/services/aigc/multimodal-generation/generation",requestMap);
        
        String imageUrl = data.getImageUrl();
        
        Map ret = new HashMap();
        ret.put("imageUrl",imageUrl);
        return ret;
    }

    public @ResponseBody Map genImage(@RequestBody Map<String,Object> questions) throws InterruptedException {
        String selectedModel = (String)questions.get("selectedModel");
        
        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "生成一颗桂花树";
        }
        ImageAgentMessage request = new ImageAgentMessage();
        request.setPrompt( message);
        ImageEvent data = null;
        if(selectedModel.equals("volcengine")){
            request.setModel( "doubao-seedream-4-5-251128");
            request.addParameter("sequential_image_generation", "disabled");
            request.addParameter("response_format", "url");
            request.addParameter("size", "2k");
            request.addParameter("watermark", true);
            data = AIAgentUtil.multimodalImageGeneration("volcengine","/api/v3/images/generations",request);
        }
        else{
            request.setModel( "qwen-image-plus");
            request.addParameter("negative_prompt","");
            request.addParameter("prompt_extend",true);
            request.addParameter("watermark",false);
            request.addParameter("size","1328*1328");
            data = AIAgentUtil.multimodalImageGeneration("qwenvlplus","/api/v1/services/aigc/multimodal-generation/generation",request);
        }
        String imageUrl = data.getImageUrl();

        Map ret = new HashMap();
        ret.put("imageUrl",imageUrl);
        return ret;
        
    }
    /**
     * curl -X POST https://ark.cn-beijing.volces.com/api/v3/images/generations \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer $ARK_API_KEY" \
     *   -d '{
     *     "model": "doubao-seedream-4-5-251128",
     *     "prompt": "星际穿越，黑洞，黑洞里冲出一辆快支离破碎的复古列车，抢视觉冲击力，电影大片，末日既视感，动感，对比色，oc渲染，光线追踪，动态模糊，景深，超现实主义，深蓝，画面通过细腻的丰富的色彩层次塑造主体与场景，质感真实，暗黑风背景的光影效果营造出氛围，整体兼具艺术幻想感，夸张的广角透视效果，耀光，反射，极致的光影，强引力，吞噬",
     *     "sequential_image_generation": "disabled",
     *     "response_format": "url",
     *     "size": "2K",
     *     "stream": false,
     *     "watermark": true
     * }'
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody Map genImageBydoubao(@RequestBody Map<String,Object> questions) throws InterruptedException {

        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "生成一颗桂花树";
        }



        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("model", "doubao-seedream-4-5-251128");
        requestMap.put("prompt", message);
        requestMap.put("sequential_image_generation", "disabled");
        requestMap.put("response_format", "url");
        requestMap.put("size", "2k");
        requestMap.put("watermark", true);


 

        ImageEvent data = AIAgentUtil.multimodalImageGeneration("volcengine","/api/v3/images/generations",requestMap);

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
                                Base64.getEncoder().encodeToString(audioBytes);
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
        Flux<List<ServerEvent>> flux = AIAgentUtil.streamChatCompletionEvent("qwenvlplus", 
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
            message = "诗歌朗诵：床前明月光；疑似地上霜；举头望明月；低头思故乡。";
        }

        AudioAgentMessage audioAgentMessage = new AudioAgentMessage();
        audioAgentMessage.setPrompt(message);
        String completionsUrl = null;
        String model = null;
            completionsUrl = "/api/v1/services/aigc/multimodal-generation/generation";
            model = "qwen3-tts-flash";
            audioAgentMessage.addParameter("voice", "Cherry")
                    .addParameter("language_type", "Chinese")
                    //设置音频下载相对路径，将和endpoint组合形成音频文件播放地址
                    .setStoreFilePathFunction(new StoreFilePathFunction() {
                        @Override
                        public String getStoreFilePath(String imageUrl) {
                            return "audio/"+SimpleStringUtil.getUUID32() +".wav";
                        }
                    });
         
        audioAgentMessage.setModel(model);
        AIAgent aiAgent = new AIAgent();
        AudioEvent audioEvent = aiAgent.genAudio("qwenvlplus",completionsUrl,audioAgentMessage);
        return audioEvent;
    }


    /**
     * 提交视频生成请求服务
     http://127.0.0.1/demoproject/reactor/submitVideoByqwenwan.api
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody Map submitVideoByqwenwan(@RequestBody Map<String,Object> questions) throws InterruptedException {
//        String selectedModel = (String)questions.get("selectedModel");

        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "一幅史诗级可爱的场景。一只小巧可爱的卡通小猫将军，身穿细节精致的金色盔甲，头戴一个稍大的头盔，勇敢地站在悬崖上。他骑着一匹虽小但英勇的战马，说：”青海长云暗雪山，孤城遥望玉门关。黄沙百战穿金甲，不破楼兰终不还。“。悬崖下方，一支由老鼠组成的、数量庞大、无穷无尽的军队正带着临时制作的武器向前冲锋。这是一个戏剧性的、大规模的战斗场景，灵感来自中国古代的战争史诗。远处的雪山上空，天空乌云密布。整体氛围是“可爱”与“霸气”的搞笑和史诗般的融合。";
        }



        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "wan2.5-t2v-preview");




        Map<String,Object> inputVoice = new LinkedHashMap();
        inputVoice.put("prompt",message);
//        inputVoice.put("audio_url","https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250923/hbiayh/%E4%BB%8E%E5%86%9B%E8%A1%8C.mp3");
        inputVoice.put("language_type","Chinese");

        requestMap.put("input",inputVoice);

        /**
         * "parameters": {
         *         "size": "832*480",
         *         "prompt_extend": true,
         *         "duration": 10,
         *         "audio": true
         *     }
         */
        Map<String,Object> parameters = new LinkedHashMap();
        parameters.put("size","832*480");
        parameters.put("prompt_extend",true);
        parameters.put("duration",10);
        parameters.put("audio",true);

        requestMap.put("parameters",parameters);
        Map header = new LinkedHashMap();
        header.put("X-DashScope-Async","enable");
        Map taskInfo = HttpRequestProxy.sendJsonBody("qwenvlplus",requestMap,"/api/v1/services/aigc/video-generation/video-synthesis",header,Map.class);


        Map<String, Object> result = new HashMap<>();
        Map output = (Map)taskInfo.get("output");
        result.put("taskId",output.get("task_id"));
        result.put("taskStatus",output.get("task_status"));
        result.put("requestId",taskInfo.get("request_id"));
        return result;
    }

    /**
     * 查询视频生成任务执行结果服务
     http://127.0.0.1/demoproject/reactor/submitVideoByqwenwan.api
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody Map getVideoTaskResult(@RequestBody Map<String,Object> questions) throws InterruptedException {
        Map<String, Object> result = new HashMap<>();
        String taskId = questions != null?(String)questions.get("taskId"):null;
        if(SimpleStringUtil.isEmpty( taskId)){
            result.put("error","taskId为空");
            return result;
        }
 
        String requestUrl = "/api/v1/tasks/"+taskId;
        Map taskInfo = HttpRequestProxy.httpGetforObject("qwenvlplus",requestUrl,Map.class);
        Map output = (Map)taskInfo.get("output");
        result.put("taskId",output.get("task_id"));
        result.put("taskStatus",output.get("task_status"));
        result.put("videoUrl",output.get("video_url"));
        result.put("requestId",taskInfo.get("request_id"));
        
        return result;
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
