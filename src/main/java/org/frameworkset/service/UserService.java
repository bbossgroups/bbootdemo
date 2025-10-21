package org.frameworkset.service;
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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author biaoping.yin
 * @Date 2025/10/20
 */
public class UserService {
    
    public Mono<User> findUserById(String id) {
        return Mono.fromCallable(() -> {
            // 模拟数据库查询
            Thread.sleep(1000); // 模拟网络延迟
            if ("123".equals(id)) {
                return new User("123", "John Doe", "john@example.com");
            } else {
                throw new RuntimeException("User not found: " + id);
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    public Mono<User> saveUser(User user) {
        return Mono.fromCallable(() -> {
            // 模拟保存操作
            Thread.sleep(500);
            return user;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}



