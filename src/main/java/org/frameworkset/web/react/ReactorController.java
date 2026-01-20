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
import org.frameworkset.spi.ai.material.StoreFilePathFunction;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.spi.ai.util.AIAgentUtil;
import org.frameworkset.spi.ai.util.AudioDataBuilder;
import org.frameworkset.spi.ai.util.MessageBuilder;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.frameworkset.util.annotations.RequestBody;
import org.frameworkset.util.annotations.RequestParam;
import org.frameworkset.util.annotations.ResponseBody;
import org.frameworkset.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author biaoping.yin
 * @Date 2025/9/30
 */
public class ReactorController implements InitializingBean {
    private Logger logger = LoggerFactory.getLogger(ReactorController.class);
     
    // 多轮会话记忆窗口：使用静态List变量模拟存储会话记忆（实际项目中建议使用数据库）
    static List<Map<String, Object>> sessionMemory = new ArrayList<>();
    /**
     * http://127.0.0.1:808/demoproject/chatpost.html
     * http://127.0.0.1:808/demoproject/reactor/deepseekChat.api?q=%E7%94%A8Java%E8%A7%A3%E9%87%8AReactor%E7%BC%96%E7%A8%8B%E6%A8%A1%E5%BC%8F
     * @param questions
     * @return
     */
    public Flux<String> deepseekChat(@RequestBody Map<String,Object> questions, @RequestParam String q) {
        String message = questions != null ?(String)questions.get("message"):q;
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
     * 使用标准化API实现流式异步/同步智能问答功能，带背压案例 和多轮会话记忆功能（完善版）
     * http://127.0.0.1/demoproject/chatBackuppressSession.html
     * @param questions
     * @return
     */
    public Flux<List<ServerEvent>> deepseekBackuppressSession(@RequestBody Map<String,Object> questions) {

        String selectedModel = (String)questions.get("selectedModel");
        Boolean reset = (Boolean) questions.get("reset");
        Boolean deepThink = (Boolean) questions.get("deepThink");
        Boolean enableStream = (Boolean) questions.get("enableStream");

        //重置会议记忆窗口
        if(reset != null && reset){
            sessionMemory.clear();
        }
        String message = (String)questions.get("message");
        ChatAgentMessage chatAgentMessage = new ChatAgentMessage();
        chatAgentMessage.setMessage( message);//当前消息
        
        //设置模型服务地址
        String completionsUrl =  null;
        String model = null;
        if(selectedModel.equals("deepseek")) {
            if(deepThink == null || !deepThink) {
                model = "deepseek-chat";
            }
            else {
                model = "deepseek-reasoner";
            }
            completionsUrl =   "/chat/completions"; //Deepseek LLM模型服务地址
            
        }
        else if(selectedModel.equals("jiutian")){
            completionsUrl =  "/largemodel/moma/api/v3/chat/completions";
            model = "jiutian-lan-comv3";
        }
        else {
            model = "qwen3-max";
            completionsUrl =  "/compatible-mode/v1/chat/completions";//通义千问LLM模型服务地址
            
            
        }
        //设置模型
        chatAgentMessage.setModel( model);
        //设置历史消息
        chatAgentMessage.setSessionMemory(sessionMemory)
        //不配置以下参数时，默认值设置如下
                .setStream( enableStream)
                .addParameter("max_tokens", 8192)
                .addParameter("temperature", 0.7);
 
       
       
        
        //提交会话请求：由enableStream参数控制流式异步/同步会话模式，true 异步  false 同步
        AIAgent aiAgent = new AIAgent();
        Flux<ServerEvent> flux = aiAgent.streamChat(selectedModel,completionsUrl,chatAgentMessage);
    
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
    
        return flux.doOnNext(chunk -> {
           
            //调试模式：输出流水会话片段到日志文件中
            if(logger.isDebugEnabled()) {
                if (!chunk.isDone()) {
                    logger.debug(chunk.getData());
                }
            }
            
        })
        .limitRate(5) //背压：限制请求速率
        .buffer(3) //缓冲：每3个元素缓冲一次
        .doOnNext(bufferedEvents -> {
            // 处理模型响应并更新会话记忆
            for(ServerEvent event : bufferedEvents) {
                //答案前后都可以添加链接和标题，实现相关知识资料链接
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
        Boolean enableStream = (Boolean) questions.get("enableStream");
        if(reset != null && reset){
            sessionMemory.clear();
        }
        Boolean deepThink = (Boolean) questions.get("deepThink");
        // enable_thinking 参数开启思考过程，thinking_budget 参数设置最大推理过程 Token 数
        if(deepThink == null)
            deepThink = true;
        String message  = null;
        message = (String)questions.get("message");
        if(SimpleStringUtil.isEmpty( message)){
            message = "介绍图片内容并计算结果";
        }
        ImageVLAgentMessage imageVLAgentMessage = new ImageVLAgentMessage();
        imageVLAgentMessage.setMessage(message);
        String model= null;
        String completionsUrl = null;

  
        if(selectedModel.equals("qwenvlplus")) {
            completionsUrl = "/compatible-mode/v1/chat/completions";
            model = "qwen3-vl-plus";
            imageVLAgentMessage.addParameter("enable_thinking", deepThink);
            imageVLAgentMessage.addParameter("thinking_budget", 81920);
        }
        else if(selectedModel.equals("volcengine")){//字节豆包
            completionsUrl =  "/api/v3/chat/completions";
            model = "doubao-seed-1-8-251228";
            //支持思考程度可调节（reasoning effort）：分为 minimal、low、medium、high 四种模式，其中minimal为不思考
            imageVLAgentMessage.addParameter("reasoning_effort", "medium");
            imageVLAgentMessage.addParameter("max_completion_tokens", 65535);
             
        }
        else if(selectedModel.equals("jiutian")){//字节豆包
            //九天模型参考文档：https://jiutian.10086.cn/portal/common-helpcenter#/document/1160?platformCode=DMX_TYZX
            completionsUrl =  "/largemodel/moma/api/v3/image/text";
            model = "LLMImage2Text";
            //支持思考程度可调节（reasoning effort）：分为 minimal、low、medium、high 四种模式，其中minimal为不思考
           

        }
         
        else{ //硅基流动图片识别服务
            model = "Qwen/Qwen3-VL-32B-Thinking";
            completionsUrl =  "/chat/completions";
            
        }

        if(enableStream) {
            imageVLAgentMessage.setStream( enableStream);
            if(selectedModel.equals("qwenvlplus")) {
                imageVLAgentMessage.addMapParameter("stream_options", "include_usage", true);
            }
        }
        imageVLAgentMessage.setModel( model);
        // 构建消息历史列表，包含之前的会话记忆
        imageVLAgentMessage.setSessionMemory(sessionMemory);

        List<String> imagesBase64  = (List)questions.get("imagesBase64");
        String imageUrl = (String)questions.get("imageUrl");
        if(imageUrl != null) {
            imageUrl = imageUrl.trim();
        }


   

        if(SimpleStringUtil.isNotEmpty(imageUrl)) {
            imageVLAgentMessage.addImageUrl(imageUrl);
        }
        if(SimpleStringUtil.isNotEmpty(imagesBase64)) {
            for(String tmp:imagesBase64) {
                imageVLAgentMessage.addImageUrl(tmp);
//                Map<String, Object> requestMap = new HashMap<>();
//                requestMap.put("model", "LLMImage2Text");
//                requestMap.put("image",tmp);
//                requestMap.put("prompt", message);
//                requestMap.put("stream", true);
//                String rsp = HttpRequestProxy.httpPostforString("jiutian", "/largemodel/moma/api/v3/image/text", requestMap);
//                logger.info(rsp);
            }
            
            
        }
 

       
        
        
//        imageVLAgentMessage.setModelType(AIConstants.AI_MODEL_TYPE_QWEN);
 
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
        Flux<List<ServerEvent>> flux = null;
        AIAgent aiAgent = new AIAgent();
       
        
        
        flux = aiAgent.streamImageParser(selectedModel,completionsUrl,imageVLAgentMessage)
                .doOnNext(chunk -> {

//                    if(!chunk.isDone()) {
//                        logger.info(chunk.getData());
//                        
//                    }
          

                })

                .limitRate(5) // 限制请求速率
                .buffer(3);
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


    

    public @ResponseBody Map genImage(@RequestBody Map<String,Object> questions) throws InterruptedException {
        String selectedModel = (String)questions.get("selectedModel");
        boolean generateMultipleImages  = questions != null?(boolean)questions.get("generateMultipleImages"):false;
        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "生成一颗桂花树";
        }
        ImageAgentMessage request = new ImageAgentMessage();
        request.setStoreFilePathFunction(new StoreFilePathFunction() {
            @Override
            public String getStoreFilePath(String imageUrl) {
                return "image/"+SimpleStringUtil.getUUID32() +".jpg";
            }
        });
        request.setMessage( message);
        ImageEvent data = null;
        AIAgent aiAgent = new AIAgent();
        String completionsUrl = null;
        if(selectedModel.equals("volcengine")){
            //字节火山引擎
            request.setModelType(AIConstants.AI_MODEL_TYPE_DOUBAO);
            request.setModel( "doubao-seedream-4-5-251128");
            if(generateMultipleImages) {
                request.addParameter("sequential_image_generation", "disabled");//生成单图
            }
            else {
                request.addParameter("sequential_image_generation", "auto");//生成多图
                request.addMapParameter("sequential_image_generation_options","max_images",3);
            }
            request.addParameter("response_format", "url");
            request.addParameter("size", "2k");
            request.addParameter("watermark", true);
            completionsUrl = "/api/v3/images/generations";
        }
        else if(selectedModel.equals("jiutian")){
            //字节火山引擎
            request.setModelType(AIConstants.AI_MODEL_TYPE_JIUTIAN);
            request.setModel( "cntxt2image");
            request.addMapParameter("image", "style","watercolor");
            request.addMapParameter("image", "ratio","3:4");
            request.addMapParameter("image", "waterMarkLevel",0);
            
            completionsUrl = "/largemodel/moma/api/v3/images/generations";
        }
        else{
            
            //阿里百炼
            //通过在http.modelType指定全局模型适配器类型，亦可以在ImageAgentMessage对象设置请求级别modelType模型适配器类型（优先级高于全局模型适配器类型）
            request.setModelType(AIConstants.AI_MODEL_TYPE_QWEN);
            request.setModel( "qwen-image-plus");
            request.addParameter("negative_prompt","");
            request.addParameter("prompt_extend",true);
            request.addParameter("watermark",false);
            request.addParameter("size","1328*1328");
            completionsUrl = "/api/v1/services/aigc/multimodal-generation/generation";
           
        }
        data = aiAgent.genImage(selectedModel,completionsUrl,request);
        String imageUrl = data.getImageUrl();

        Map ret = new HashMap();
        ret.put("imageUrl",imageUrl);
        return ret;
        
    }


    /**
     * 根据图文修改图片
     * @param questions
     * @return
     * @throws InterruptedException
     */

    public @ResponseBody Map genImageFromImage(@RequestBody Map<String,Object> questions) throws InterruptedException {
        String selectedModel = (String)questions.get("selectedModel");
        boolean generateMultipleImages  = questions != null?(boolean)questions.get("generateMultipleImages"):false;
        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "生成一颗桂花树";
        }
        ImageAgentMessage request = new ImageAgentMessage();
        request.setStoreFilePathFunction(new StoreFilePathFunction() {
            @Override
            public String getStoreFilePath(String imageUrl) {
                return "image/"+SimpleStringUtil.getUUID32() +".jpg";
            }
        });

        List<String> imagesBase64  = (List)questions.get("imagesBase64");
        String imageUrl = (String)questions.get("imageUrl");
        if(imageUrl != null) {
            imageUrl = imageUrl.trim();
        }




        if(SimpleStringUtil.isNotEmpty(imageUrl)) {
            request.addImageUrl(imageUrl);
        }
        if(SimpleStringUtil.isNotEmpty(imagesBase64)) {
            for(String tmp:imagesBase64) {
                request.addImageUrl(tmp);
//                Map<String, Object> requestMap = new HashMap<>();
//                requestMap.put("model", "LLMImage2Text");
//                requestMap.put("image",tmp);
//                requestMap.put("prompt", message);
//                requestMap.put("stream", true);
//                String rsp = HttpRequestProxy.httpPostforString("jiutian", "/largemodel/moma/api/v3/image/text", requestMap);
//                logger.info(rsp);
            }


        }
        request.setMessage( message);
        ImageEvent data = null;
        AIAgent aiAgent = new AIAgent();
        String completionsUrl = null;
        if(selectedModel.equals("volcengine")){
            //字节火山引擎
            request.setModelType(AIConstants.AI_MODEL_TYPE_DOUBAO);
            request.setModel( "doubao-seedream-4-5-251128");
            if(generateMultipleImages) {
                request.addParameter("sequential_image_generation", "disabled");//生成单图
            }
            else {
                request.addParameter("sequential_image_generation", "auto");//生成多图
                request.addMapParameter("sequential_image_generation_options","max_images",3);
            }
            request.addParameter("response_format", "url");
            request.addParameter("size", "2k");
            request.addParameter("watermark", true);
            completionsUrl = "/api/v3/images/generations";
        }
        else if(selectedModel.equals("jiutian")){
            //字节火山引擎
            request.setModelType(AIConstants.AI_MODEL_TYPE_JIUTIAN);
            request.setModel( "cntxt2image");
            completionsUrl = "/largemodel/moma/api/v3/images/generations";
        }
        else{

            //阿里百炼
            //通过在http.modelType指定全局模型适配器类型，亦可以在ImageAgentMessage对象设置请求级别modelType模型适配器类型（优先级高于全局模型适配器类型）
            request.setModelType(AIConstants.AI_MODEL_TYPE_QWEN);
            request.setModel( "qwen-image-edit-max-2026-01-16");
            request.addParameter("n",2);
            request.addParameter("prompt_extend",true);
            request.addParameter("watermark",false);
            request.addParameter("size","1536*1024");
            ///api/v1/services/aigc/multimodal-generation/generation
            completionsUrl = "/api/v1/services/aigc/multimodal-generation/generation";

        }
        data = aiAgent.genImage(selectedModel,completionsUrl,request);
        String newimageUrl = data.getImageUrl();

        Map ret = new HashMap();
        ret.put("imageUrl",newimageUrl);
        ret.put("imageUrls",data.getImageUrls());
        ret.put("response",data.getResponse());
        ret.put("contentEvent",data.getContentEvent());
        return ret;

    }


    public @ResponseBody Map genImage1(@RequestBody Map<String,Object> questions) throws InterruptedException {

        
         
        ImageAgentMessage request = new ImageAgentMessage();
        //设置生成图片的文本内容（提示词）
        String message  = "生成一美女";
        request.setMessage( message);        
        //设置字节火山引擎豆包图片生成模型
        request.setModel( "doubao-seedream-4-5-251128");
        
        //设置字节火山引擎豆包图片生成模型特有参数
        request.addParameter("sequential_image_generation", "disabled");
        request.addParameter("response_format", "url");
        request.addParameter("size", "2k");
        request.addParameter("watermark", true);
        //执行通用AI客户端工具标准化图片生成API，指定模型服务名称volcengine和字节火山引擎豆包图片生成模型服务地址，并提交图片生成请求参数request
        ImageEvent data = AIAgentUtil.multimodalImageGeneration("volcengine","/api/v3/images/generations",request);
        
        //获取生成的图片url，可以直接在浏览器中展示
        String imageUrl = data.getImageUrl();

        Map ret = new HashMap();
        ret.put("imageUrl",imageUrl);
        return ret;

    }

    public @ResponseBody Map genImage2(@RequestBody Map<String,Object> questions) throws InterruptedException {



        ImageAgentMessage request = new ImageAgentMessage();
        //设置生成图片的文本内容（提示词）
        String message  = "生成一位美女";
        request.setMessage( message);
        //设置通义千问图片生成模型
        request.setModel( "qwen-image-plus");

        //设置通义千问图片生成模型特有参数
        request.addParameter("negative_prompt","");
        request.addParameter("prompt_extend",true);
        request.addParameter("watermark",false);
        request.addParameter("size","1328*1328");
        //执行通用AI客户端工具标准化图片生成API，指定模型服务名称qwenvlplus和通义千问图片图片生成模型服务地址，并提交图片生成请求参数request
        ImageEvent data = AIAgentUtil.multimodalImageGeneration("qwenvlplus","/api/v1/services/aigc/multimodal-generation/generation",request);

        //获取生成的图片url，可以直接在浏览器中展示
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



        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "qwen3-tts-flash");




        Map<String,Object> inputVoice = new LinkedHashMap();
        inputVoice.put("text",message);
        inputVoice.put("voice","Cherry");
        inputVoice.put("language_type","Chinese");

        requestMap.put("input",inputVoice);
        AudioEvent audioEvent = AIAgentUtil.multimodalAudioGeneration("qwenvlplus","/api/v1/services/aigc/multimodal-generation/generation",requestMap);



//
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
        //加载模型服务配置文件，启动模型服务智能体工具
        HttpRequestProxy.startHttpPools("application.properties");
    }
}
