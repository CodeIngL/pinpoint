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
package com.navercorp.pinpoint.bootstrap.interceptor;

import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

/**
 * 拦截器工具类
 * @author Jongho Moon
 *
 */
public class InterceptorInvokerHelper {
    private static boolean propagateException = false;
    private static final PLogger logger = PLoggerFactory.getLogger(InterceptorInvokerHelper.class.getName());

    /**
     * 是否传播异常，是在包装为运行是异常（可能对某些场景不合适，不想要运行是异常）
     * 是争取的，因为这个异常发生在拦截器中，所以是合理的
     * @param t
     */
    public static void handleException(Throwable t) {
        if (propagateException) {
            throw new RuntimeException(t);
        } else {
            logger.warn("Exception occurred from interceptor", t);
        }
    }
    
    public static void setPropagateException(boolean propagate) {
        propagateException = propagate;
    }
}
