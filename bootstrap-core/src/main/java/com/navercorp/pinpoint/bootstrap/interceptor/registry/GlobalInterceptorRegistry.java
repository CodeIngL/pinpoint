/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.interceptor.registry;

import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;


/**
 * for test
 * test的目的，实际上没有使用
 * @author emeroad
 */
public class GlobalInterceptorRegistry {

    public static final InterceptorRegistryAdaptor REGISTRY = new DefaultInterceptorRegistryAdaptor();

    public static void bind(final InterceptorRegistryAdaptor interceptorRegistryAdaptor, final Object lock) {

    }

    public static void unbind(final Object lock) {

    }

    public static Interceptor getInterceptor(int key) {
        return REGISTRY.getInterceptor(key);
    }
}
