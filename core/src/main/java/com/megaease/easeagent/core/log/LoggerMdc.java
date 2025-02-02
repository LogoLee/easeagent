/*
 * Copyright (c) 2017, MegaEase
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

package com.megaease.easeagent.core.log;

import com.megaease.easeagent.plugin.api.logging.Mdc;

public class LoggerMdc implements Mdc {
    private final com.megaease.easeagent.log4j2.api.Mdc mdc;

    public LoggerMdc(com.megaease.easeagent.log4j2.api.Mdc mdc) {
        this.mdc = mdc;
    }

    @Override
    public void put(String key, String value) {
        mdc.put(key, value);
    }

    @Override
    public void remove(String key) {
        mdc.remove(key);
    }

    @Override
    public String get(String key) {
        return mdc.get(key);
    }
}
