/*
 * Copyright 2016 NAVER Corp.
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

/**
 * 异常处理，特殊的拦截器，带有apiId，组合真正的拦截器
 * @author jaehong.kim
 */
public class ExceptionHandleApiIdAwareAroundInterceptor implements ApiIdAwareAroundInterceptor {

    private final ApiIdAwareAroundInterceptor delegate;

    public ExceptionHandleApiIdAwareAroundInterceptor(ApiIdAwareAroundInterceptor delegate) {
        if (delegate == null) {
            throw new NullPointerException("delegate must not be null");
        }

        this.delegate = delegate;
    }

    @Override
    public void before(Object target, int apiId, Object[] args) {
        try {
            this.delegate.before(target, apiId, args);
        } catch (Throwable t) {
            InterceptorInvokerHelper.handleException(t);
        }
    }

    @Override
    public void after(Object target, int apiId, Object[] args, Object result, Throwable throwable) {
        try {
            this.delegate.after(target, apiId, args, result, throwable);
        } catch (Throwable t) {
            InterceptorInvokerHelper.handleException(t);
        }
    }
}