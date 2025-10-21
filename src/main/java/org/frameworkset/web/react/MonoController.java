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

import org.frameworkset.service.User;
import org.frameworkset.service.UserService;
import reactor.core.publisher.Mono;

/**
 * @author biaoping.yin
 * @Date 2025/10/20
 */
public class MonoController {
    private UserService userService = new UserService();

    /**
     * 获取用户信息
     * http://127.0.0.1/demoproject/reactor/mono/getUser.api?id=123
     * @param id
     * @return
     */
    public Mono<User> getUser(String id){
        return userService.findUserById(id);
    }

}
