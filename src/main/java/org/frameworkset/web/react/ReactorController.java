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
import org.frameworkset.util.annotations.RequestParam;
import org.frameworkset.util.annotations.ResponseBody;
import org.frameworkset.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
        chatAgentMessage.setPrompt( message);//当前消息
        
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
        else if(selectedModel.equals("kimi")){
            completionsUrl =  "/v1/chat/completions";
            
            
            if(deepThink == null || !deepThink) {
                model = "kimi-k2-turbo-preview";
            }
            else {
                model = "kimi-k2-thinking";
            }
        }
        else if(selectedModel.equals("zhipu")){
            completionsUrl =  "/api/paas/v4/chat/completions";

            model = "glm-4.7";
            if(deepThink != null && deepThink) {
                chatAgentMessage.addMapParameter("thinking","type","enabled");
            }
            else{
                chatAgentMessage.addMapParameter("thinking","type","disabled");
            }
        }
        
        else {
            model = "qwen3-max";
            completionsUrl =  "/compatible-mode/v1/chat/completions";//通义千问LLM模型服务地址
            
            
        }
        //设置模型
        chatAgentMessage.setModel( model);
        //设置历史消息
        chatAgentMessage.setSessionMemory(sessionMemory).setSessionSize(50)
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
                        chatAgentMessage.addSessionMessage(completeAnswer.toString());
                        
                       
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
        imageVLAgentMessage.setPrompt(message);
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
        else if(selectedModel.equals("kimi")){//字节豆包
            completionsUrl =  "/v1/chat/completions";
            model = "moonshot-v1-8k-vision-preview";
            //支持思考程度可调节（reasoning effort）：分为 minimal、low、medium、high 四种模式，其中minimal为不思考
            imageVLAgentMessage.setTemperature(0.6);

        }
        else if(selectedModel.equals("zhipu")){//字节豆包
            completionsUrl =  "/api/paas/v4/chat/completions";
            model = "glm-4.6v";
            //支持思考程度可调节（reasoning effort）：分为 minimal、low、medium、high 四种模式，其中minimal为不思考
            if(deepThink != null && deepThink) {
                imageVLAgentMessage.addMapParameter("thinking","type","enabled");
            }
            else{
                imageVLAgentMessage.addMapParameter("thinking","type","disabled");
            }
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
        imageVLAgentMessage.setSessionSize(50);

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
                                imageVLAgentMessage.addSessionMessage(completeAnswer.toString());
                               
                            }


                        }
                    }
                });
        
        return flux;
    }


    

    public @ResponseBody ImageEvent genImage(@RequestBody Map<String,Object> questions) throws InterruptedException {
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
        request.setPrompt( message);
        ImageEvent data = null;
        AIAgent aiAgent = new AIAgent();
        String completionsUrl = null;
        if(selectedModel.equals("volcengine")){
            //字节火山引擎
//            request.setModelType(AIConstants.AI_MODEL_TYPE_DOUBAO);
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
            request.setModel( "cntxt2image");
            request.addMapParameter("image", "style","watercolor");
            request.addMapParameter("image", "ratio","3:4");
            request.addMapParameter("image", "waterMarkLevel",0);
            
            completionsUrl = "/largemodel/moma/api/v3/images/generations";
        }
        else if(selectedModel.equals("zhipu")){
            //字节火山引擎
            request.setModel( "glm-image");
            request.addParameter("size","1280x1280");           

            completionsUrl = "/api/paas/v4/images/generations";
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
        return data;
        
    }


    /**
     * 根据图文修改图片
     * @param questions
     * @return
     * @throws InterruptedException
     */

    public @ResponseBody ImageEvent genImageFromImage(@RequestBody Map<String,Object> questions) throws InterruptedException {
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
        request.setPrompt( message);
        ImageEvent data = null;
        AIAgent aiAgent = new AIAgent();
        String completionsUrl = null;
        if(selectedModel.equals("volcengine")){
            //字节火山引擎
            request.setModelType(AIConstants.AI_MODEL_TYPE_DOUBAO);
            request.setModel( "doubao-seedream-4-5-251128");
            if(!generateMultipleImages) {
                request.addParameter("sequential_image_generation", "disabled");//生成单图
            }
            else {
                request.addParameter("sequential_image_generation", "auto");//生成多图
                request.addMapParameter("sequential_image_generation_options","max_images",5);
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
            request.addParameter("n",5);
            request.addParameter("prompt_extend",true);
            request.addParameter("watermark",false);
            request.addParameter("size","1536*1024");
            ///api/v1/services/aigc/multimodal-generation/generation
            completionsUrl = "/api/v1/services/aigc/multimodal-generation/generation";

        }
        data = aiAgent.genImage(selectedModel,completionsUrl,request);
        return data;

    }


     
	/**
     * 音频识别功能
     * https://bailian.console.aliyun.com/?spm=5176.29597918.J_SEsSjsNv72yRuRFS2VknO.2.74ba7b08ig5jxD&tab=doc#/doc/?type=model&url=2979031
     * @param audio 音频文件
     * @param request HTTP请求
     * @return
     */
    public Flux<List<ServerEvent>> audioFileRecognition(MultipartFile audio, HttpServletRequest request) throws IOException {
        String selectedModel = request.getParameter("selectedModel");
        String reset = request.getParameter("reset");
        Boolean enableStream = false;
        String t = request.getParameter("enableStream");
        if(t != null && t.equals("true")){
            enableStream = true;
        }
        if (reset != null && reset.equals("true")) {
            sessionMemory.clear();
        }
        String message = null;
        message = request.getParameter("message");
        if (SimpleStringUtil.isEmpty(message)) {
            message = "介绍音频内容";
        }
    
        AudioSTTAgentMessage audioSTTAgentMessage = new AudioSTTAgentMessage();
        audioSTTAgentMessage.setStream(enableStream);
        String model = null;
        String completionUrl = null;
        audioSTTAgentMessage.setAudio(audio.getBytes());
        audioSTTAgentMessage.setContentType(audio.getContentType());
        if(selectedModel.equals("qwenvlplus")){
            model = "qwen3-asr-flash";
            completionUrl = "/api/v1/services/aigc/multimodal-generation/generation";
                //设置音频文件内容
                
                //直接设置音频url地址
//       audioSTTAgentMessage.setAudio("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3");

            
            audioSTTAgentMessage.addMapParameter("asr_options","enable_itn",true);
            audioSTTAgentMessage.addParameter("incremental_output", true);
            audioSTTAgentMessage.setResultFormat( "message");
            
        }
        else if(selectedModel.equals("zhipu")){
            model = "glm-asr-2512";
            completionUrl = "/api/paas/v4/audio/transcriptions";
        }
        audioSTTAgentMessage.setModel(model);
        // 构建消息历史列表，包含之前的会话记忆,语音识别模型本身无法实现多轮会话，如果要多轮会话，需切换支持多轮会话的模型，例如LLM和千问图片识别模型
        audioSTTAgentMessage.setSessionMemory(sessionMemory);
        audioSTTAgentMessage.setSessionSize(50);
        // 添加当前用户消息
        audioSTTAgentMessage.setPrompt( message);
        
        // 用于累积完整的回答
        StringBuilder completeAnswer = new StringBuilder();
        AIAgent aiAgent = new AIAgent();
        Flux<List<ServerEvent>> flux = aiAgent.streamAudioParser(selectedModel,
                        completionUrl, audioSTTAgentMessage)
//                .doOnNext(chunk -> {
//                    if (!chunk.isDone()) {
//                        logger.info(chunk.getData());
//                    }
//                })
                .limitRate(5) // 限制请求速率
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
                        audioSTTAgentMessage.addSessionMessage(completeAnswer.toString());

                    }
                }
            }
        });
    
        return flux;
    }

    /**
     * 音频生成服务
     http://127.0.0.1/demoproject/reactor/genAudioByqwentts.api
     https://bailian.console.aliyun.com/cn-beijing/?spm=5176.29597918.J_SEsSjsNv72yRuRFS2VknO.2.74ba7b08ig5jxD&tab=doc#/doc/?type=model&url=2879134
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody AudioEvent genAudioByqwentts(@RequestBody Map<String,Object> questions) {
        String selectedModel = (String)questions.get("selectedModel");

        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "诗歌朗诵：床前明月光；疑似地上霜；举头望明月；低头思故乡。";
        }
        AudioAgentMessage audioAgentMessage = new AudioAgentMessage();
        audioAgentMessage.setPrompt(message);
        String completionsUrl = null;
        String model = null;
        if(selectedModel.equals("qwenvlplus")) {
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
        }
        else if(selectedModel.equals("zhipu")) {
            //https://docs.bigmodel.cn/api-reference/%E6%A8%A1%E5%9E%8B-api/%E6%96%87%E6%9C%AC%E8%BD%AC%E8%AF%AD%E9%9F%B3
            completionsUrl = "/api/paas/v4/audio/speech";
            model = "glm-tts";
            audioAgentMessage.addParameter("voice", "female")
                    .addParameter("response_format", "wav")
                    .addParameter("speed", 1.0)
                    .addParameter("volume", 1.0)            
                    //设置音频下载相对路径，将和endpoint组合形成音频文件播放地址            
                    .setStoreFilePathFunction(new ReponseStoreFilePathFunction() {
                        @Override
                        public String getStoreFilePath(String imageUrl) {
                            return "audio/"+SimpleStringUtil.getUUID32() +".wav";
                        }
                    });
        }
        audioAgentMessage.setModel(model);    
        AIAgent aiAgent = new AIAgent();
        AudioEvent audioEvent = aiAgent.genAudio(selectedModel,completionsUrl,audioAgentMessage);
        return audioEvent;
    }

    /**
     * 音频生成服务
     http://127.0.0.1/demoproject/reactor/genAudioByqwentts.api
     https://bailian.console.aliyun.com/cn-beijing/?spm=5176.29597918.J_SEsSjsNv72yRuRFS2VknO.2.74ba7b08ig5jxD&tab=doc#/doc/?type=model&url=2879134
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public Flux<List<ServerEvent>> streamGenAudioByqwentts(@RequestBody Map<String,Object> questions) throws InterruptedException, FileNotFoundException {
        String selectedModel = (String)questions.get("selectedModel");

        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "诗歌朗诵：床前明月光；疑似地上霜；举头望明月；低头思故乡。";
        }
        String completionsUrl = null;
        String model = null;
        AudioAgentMessage audioAgentMessage = new AudioAgentMessage();
        audioAgentMessage.setPrompt(message);
        if(selectedModel.equals("qwenvlplus")) {
           
            model = "qwen3-tts-flash";
            completionsUrl = "/api/v1/services/aigc/multimodal-generation/generation";            
            audioAgentMessage.addParameter("voice", "Cherry")
                    .addParameter("language_type", "Chinese");
            //设置音频下载相对路径，将和endpoint组合形成音频文件播放地址
            audioAgentMessage.setStoreFilePathFunction(new StoreFilePathFunction() {
                @Override
                public String getStoreFilePath(String imageUrl) {
                    return "audio/" + SimpleStringUtil.getUUID32() + ".wav";
                }
            });
        }
        
        else if(selectedModel.equals("zhipu")) {
            //https://docs.bigmodel.cn/api-reference/%E6%A8%A1%E5%9E%8B-api/%E6%96%87%E6%9C%AC%E8%BD%AC%E8%AF%AD%E9%9F%B3
            completionsUrl = "/api/paas/v4/audio/speech";
            model = "glm-tts";
            audioAgentMessage.addParameter("voice", "female")
                    .addParameter("response_format", "pcm")
                    .addParameter("encode_format", "base64")
                    .addParameter("speed", 1.0)
                    .addParameter("volume", 1.0);
             
        }
        
        audioAgentMessage.setModel(model);
        audioAgentMessage.setStream(true);
        
        AIAgent aiAgent = new AIAgent();
        Flux<ServerEvent> flux = aiAgent.streamAudioGen(selectedModel,completionsUrl,audioAgentMessage);
//        FileOutputStream fos = new FileOutputStream("C:\\data\\ai\\aigenfiles\\audio/audio.wav");
        return flux
//                .doOnNext(chunk -> {
//                    if (!chunk.isDone()) {
//                        logger.info(chunk.getData());
//                        
//                    }
//                    else{
//                        
//                    }
//                })
                .limitRate(5) //背压：限制请求速率
                .buffer(3) //缓冲：每3个元素缓冲一次
                .doOnNext(bufferedEvents -> {
                    // 处理模型响应并更新会话记忆
                    for(ServerEvent event : bufferedEvents) {
                        //答案前后都可以添加链接和标题，实现相关知识资料链接
                        if(event.isFirst()){
                            event.addExtendData("url","https://www.bbossgroups.com");
                            event.addExtendData("title","bboss官网");
                            
                        }
                        if(event.isDone() && event.getUrl() != null){
                            event.addExtendData("url",event.getUrl());
                            event.addExtendData("title","下载音频");

                        }
                        boolean execute = false;
                        if(!event.isDone() && execute){
                            try {
                                //直接在服务端播放语音

                                byte[] audioBytes = Base64.getDecoder().decode(event.getData());
                                // 2. 配置音频格式（根据API返回的音频格式调整）
                                AudioFormat format = new AudioFormat(
                                        AudioFormat.Encoding.PCM_SIGNED,
                                        24000, // 采样率（需与API返回格式一致）
                                        16,    // 采样位数
                                        1,     // 声道数
                                        2,     // 帧大小（位数/字节数）
                                        24000, // 数据传输率（需与采样率一致）
                                        false  // 是否压缩
                                );

                                // 3. 实时播放音频数据
                                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                                    if (line != null) {
                                        line.open(format);
                                        line.start();
                                        line.write(audioBytes, 0, audioBytes.length);
                                        line.drain();
                                    }
                                }
                            }
                            catch (Exception e){
                                logger.error("播放音频数据异常",e);
                            }
                        }
                         
                    }
                });
    }


    /**
     * 提交视频生成请求服务
     http://127.0.0.1/demoproject/reactor/submitVideoByqwenwan.api
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody VideoTask submitVideoByqwenwan(@RequestBody Map<String,Object> questions) throws InterruptedException {
//        String selectedModel = (String)questions.get("selectedModel");

        String message  = null;
        message = questions != null?(String)questions.get("message"):null;
        if(SimpleStringUtil.isEmpty( message)){
            message = "一幅史诗级可爱的场景。一只小巧可爱的卡通小猫将军，身穿细节精致的金色盔甲，头戴一个稍大的头盔，勇敢地站在悬崖上。他骑着一匹虽小但英勇的战马，说：”青海长云暗雪山，孤城遥望玉门关。黄沙百战穿金甲，不破楼兰终不还。“。悬崖下方，一支由老鼠组成的、数量庞大、无穷无尽的军队正带着临时制作的武器向前冲锋。这是一个戏剧性的、大规模的战斗场景，灵感来自中国古代的战争史诗。远处的雪山上空，天空乌云密布。整体氛围是“可爱”与“霸气”的搞笑和史诗般的融合。";
        }
        List<String> imagesBase64  = (List)questions.get("imagesBase64");
        String imageUrl = (String)questions.get("imageUrl");
        if(imageUrl != null) {
            imageUrl = imageUrl.trim();
        }




        
        VideoAgentMessage videoAgentMessage = new VideoAgentMessage();
        String model = null;
        if(SimpleStringUtil.isNotEmpty(imageUrl)) {
            if(imageUrl.indexOf(",") > 0){
                String[] imageUrls = imageUrl.split(",");
                videoAgentMessage.setFirstFrameUrl(imageUrls[0]);
                videoAgentMessage.setLastFrameUrl(imageUrls[1]);
                model = "wan2.2-kf2v-flash";
            }
            else {
                videoAgentMessage.setImgUrl(imageUrl);
                videoAgentMessage.setAudioUrl("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250923/hbiayh/%E4%BB%8E%E5%86%9B%E8%A1%8C.mp3");
                model = "wan2.6-i2v-flash";
            }
        }
        else if(imagesBase64 != null && imagesBase64.size() > 0){
            if(imagesBase64.size() > 1){
                videoAgentMessage.setFirstFrameUrl(imagesBase64.get(0));
                videoAgentMessage.setLastFrameUrl(imagesBase64.get(1));
                model = "wan2.2-kf2v-flash";
            }
            else{
                videoAgentMessage.setImgUrl(imagesBase64.get(0));
                videoAgentMessage.setAudioUrl("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250923/hbiayh/%E4%BB%8E%E5%86%9B%E8%A1%8C.mp3");
                model = "wan2.6-i2v-flash";
            }
           
        }
        else{
            videoAgentMessage.setAudioUrl("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250923/hbiayh/%E4%BB%8E%E5%86%9B%E8%A1%8C.mp3");
            model = "wan2.6-t2v";
        }
        
       
        if(videoAgentMessage.getImgUrl() != null){
           
            videoAgentMessage                     
                    .addParameter("resolution","1080P")
                    .addParameter("prompt_extend",true)
                    .addParameter("duration",10)
                    .addParameter("watermark",true)
                    .addParameter("shot_type","multi")
                    .setTemplate("hanfu-1");
        }
        else  if(videoAgentMessage.getFirstFrameUrl() != null){

            videoAgentMessage
                    .addParameter("resolution","1080P")
                    .addParameter("prompt_extend",true)
                    
                   ;
        }
        else{
           
            videoAgentMessage                     
                    .addParameter("size","1280*720")
                    .addParameter("prompt_extend",true)
                    .addParameter("duration",10)
                    .addParameter("shot_type","multi");
        }
        videoAgentMessage.setModel(model);
        videoAgentMessage.setPrompt( message)
                ;
       AIAgent aiAgent = new AIAgent();
       VideoTask videoTask = aiAgent.submitVideoTask("qwenvlplus",videoAgentMessage);
      
        return videoTask;
    }

    /**
     * 查询视频生成任务执行结果服务
     http://127.0.0.1/demoproject/reactor/submitVideoByqwenwan.api
     * @param questions
     * @return
     * @throws InterruptedException
     */
    public @ResponseBody VideoGenResult getVideoTaskResult(@RequestBody Map<String,Object> questions) throws InterruptedException {
        String taskId = questions != null?(String)questions.get("taskId"):null;
         
        AIAgent aiAgent = new AIAgent();
        VideoStoreAgentMessage videoStoreAgentMessage = new VideoStoreAgentMessage();
        videoStoreAgentMessage.setTaskId(taskId);
        videoStoreAgentMessage.setStoreFilePathFunction(new StoreFilePathFunction() {
            @Override
            public String getStoreFilePath(String imageUrl) {
                return "video/" + SimpleStringUtil.getUUID32() + ".mp4";
            }
        });
        VideoGenResult videoTaskResult = aiAgent.getVideoTaskResult("qwenvlplus", videoStoreAgentMessage);
         
//        String requestUrl = "/api/v1/tasks/"+taskId;
//        Map taskInfo = HttpRequestProxy.httpGetforObject("qwenvlplus",requestUrl,Map.class);
//        Map output = (Map)taskInfo.get("output");
//        result.put("taskId",output.get("task_id"));
//        result.put("taskStatus",output.get("task_status"));
//        result.put("videoUrl",output.get("video_url"));
//        result.put("requestId",taskInfo.get("request_id"));
        
        return videoTaskResult;
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
