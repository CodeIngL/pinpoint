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
package com.navercorp.pinpoint.bootstrap.interceptor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.navercorp.pinpoint.bootstrap.instrument.MethodFilter;

/**
 * Specify the {@link MethodFilter} which will be used to filter the annotated interceptor's target methods.
 *
 * 指定将用于过滤注解的拦截器的目标方法的MethodFilter。
 * @author Jongho Moon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TargetFilter {
    /**
     * Filter type
     */
    String type();
    
    /**
     * Arguments for specified {@link MethodFilter}'s constructor. 
     */
    String[] constructorArguments() default {};
    
    boolean singleton() default false;
}
