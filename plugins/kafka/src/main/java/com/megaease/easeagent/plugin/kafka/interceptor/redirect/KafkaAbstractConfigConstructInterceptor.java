/*
 * Copyright (c) 2021, MegaEase
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.megaease.easeagent.plugin.kafka.interceptor.redirect;

import com.megaease.easeagent.plugin.MethodInfo;
import com.megaease.easeagent.plugin.api.Context;
import com.megaease.easeagent.plugin.api.middleware.MiddlewareConfigProcessor;
import com.megaease.easeagent.plugin.api.middleware.ResourceConfig;
import com.megaease.easeagent.plugin.enums.Order;
import com.megaease.easeagent.plugin.interceptor.NonReentrantInterceptor;

import java.util.Map;
import java.util.Properties;

public class KafkaAbstractConfigConstructInterceptor implements NonReentrantInterceptor {

    @Override
    public void doBefore(MethodInfo methodInfo, Context context) {
        ResourceConfig cnf = MiddlewareConfigProcessor.INSTANCE.getData(MiddlewareConfigProcessor.ENV_KAFKA);
        if (cnf == null) {
            return;
        }
        if (methodInfo.getArgs()[0] instanceof Properties) {
            Properties properties = (Properties) methodInfo.getArgs()[0];
            properties.put("bootstrap.servers", cnf.getUris());
            methodInfo.changeArg(0, properties);
        } else if (methodInfo.getArgs()[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) methodInfo.getArgs()[0];
            map.put("bootstrap.servers", cnf.getUris());
            methodInfo.changeArg(0, map);
        }
    }


    @Override
    public String getType() {
        return Order.REDIRECT.getName();
    }
}
