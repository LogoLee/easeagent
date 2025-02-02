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

package com.megaease.easeagent.core.context;

import java.util.HashMap;
import java.util.Map;

public class RetBound  {
    int size;
    Map<String, Object> local;

    RetBound(int size) {
        this.size = size;
    }

    public int size() {
        return this.size;
    }

    public Object get(String key) {
        if (local == null) {
            return null;
        }
        return local.get(key);
    }

    public void put(String key, Object value) {
        if (local == null) {
            this.local = new HashMap<>();
        }
        this.local.put(key, value);
    }
}
