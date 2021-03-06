/*
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.bootstrap.instrument;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScope;

/**
 *
 * @author Jongho Moon
 *
 */
public interface Instrumentor {

    ProfilerConfig getProfilerConfig();
    
    InstrumentClass getInstrumentClass(ClassLoader classLoader, String className, byte[] classfileBuffer);
    
    boolean exist(ClassLoader classLoader, String className);
    
    InterceptorScope getInterceptorScope(String scopeName);
        
    <T> Class<? extends T> injectClass(ClassLoader targetClassLoader, String className);
    
    void transform(ClassLoader classLoader, String targetClassName, TransformCallback transformCallback);

    /**
     * 重新转换
     * @param target
     * @param transformCallback
     */
    void retransform(Class<?> target, TransformCallback transformCallback);
}
