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

package com.megaease.easeagent.core.plugin.interceptor;

import com.megaease.easeagent.plugin.AgentPlugin;
import com.megaease.easeagent.plugin.Interceptor;
import com.megaease.easeagent.plugin.Provider;

import java.util.function.Supplier;

public class ProviderPluginDecorator implements Provider {
    private final AgentPlugin plugin;
    private final Provider provider;

    public ProviderPluginDecorator(AgentPlugin plugin, Provider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @Override
    public Supplier<Interceptor> getInterceptorProvider() {
        return new Supplier<Interceptor>() {
            @Override
            public Interceptor get() {
                Supplier<Interceptor> origin = ProviderPluginDecorator.this.provider.getInterceptorProvider();
                AgentPlugin plugin = ProviderPluginDecorator.this.plugin;
                return new InterceptorPluginDecorator(origin.get(), plugin);
            }
        };
    }

    @Override
    public String getAdviceTo() {
        return this.provider.getAdviceTo();
    }

    @Override
    public String getPluginClassName() {
        return this.provider.getPluginClassName();
    }
}
