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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author biaoping.yin
 * @Date 2025/9/30
 */
public class ReactorController {
    private Logger logger = LoggerFactory.getLogger(ReactorController.class);
    public Flux<String> chatTest() {

//		NettyWebServer nettyWebServer = new NettyWebServer();
        logger.info("");

        Flux<String> flux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> {
                    String data = "data: "+ i +"," + LocalDateTime.now() + "\n\n";
                    System.out.println(data);
                    return data;
                })
                .take(10); // 限制发送10条数据
        return flux;
        //chat交互测试
//        return Flux.interval(Duration.ofSeconds(1)).take(10).map(sequence -> "{" + "    \"data\": \"33\"," + "    \"count\": \"" + sequence + "\"" + "}");
    }

}
