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

package com.navercorp.pinpoint.profiler.instrument.interceptor;

import com.navercorp.pinpoint.bootstrap.interceptor.*;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.IgnoreMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 拦截器定义工厂
 * @author Woonduk Kang(emeroad)
 */
public class InterceptorDefinitionFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    //类型检测
    private final List<TypeHandler> detectHandlers;

    public InterceptorDefinitionFactory() {
        this.detectHandlers = register();
    }

    /**
     * 生成拦截器定义
     * @param interceptorClazz 拦截器类
     * @return
     */
    public InterceptorDefinition createInterceptorDefinition(Class<?> interceptorClazz) {
        if (interceptorClazz == null) {
            throw new NullPointerException("targetInterceptorClazz must not be null");
        }

        //解析拦截器从哪个拦截器派生
        for (TypeHandler typeHandler : detectHandlers) {
            final InterceptorDefinition interceptorDefinition = typeHandler.resolveType(interceptorClazz);
            if (interceptorDefinition != null) {
                return interceptorDefinition;
            }
        }
        throw new RuntimeException("unsupported Interceptor Type. " + interceptorClazz.getName());
    }


    /**
     * 注册各类拦截器方法
     * @return
     */
    private List<TypeHandler> register() {
        final List<TypeHandler> typeHandlerList = new ArrayList<TypeHandler>();

        addTypeHandler(typeHandlerList, AroundInterceptor.class, InterceptorType.ARRAY_ARGS);

        addTypeHandler(typeHandlerList, AroundInterceptor0.class, InterceptorType.BASIC);
        addTypeHandler(typeHandlerList, AroundInterceptor1.class, InterceptorType.BASIC);
        addTypeHandler(typeHandlerList, AroundInterceptor2.class, InterceptorType.BASIC);
        addTypeHandler(typeHandlerList, AroundInterceptor3.class, InterceptorType.BASIC);
        addTypeHandler(typeHandlerList, AroundInterceptor4.class, InterceptorType.BASIC);
        addTypeHandler(typeHandlerList, AroundInterceptor5.class, InterceptorType.BASIC);


        addTypeHandler(typeHandlerList, StaticAroundInterceptor.class, InterceptorType.STATIC);

        addTypeHandler(typeHandlerList, ApiIdAwareAroundInterceptor.class, InterceptorType.API_ID_AWARE);

        return typeHandlerList;
    }

    private void addTypeHandler(List<TypeHandler> typeHandlerList, Class<? extends Interceptor> interceptorClazz, InterceptorType arrayArgs) {
        final TypeHandler typeHandler = createInterceptorTypeHandler(interceptorClazz, arrayArgs);
        typeHandlerList.add(typeHandler);
    }

    /**
     * 构建TypeHandler
     * @param interceptorClazz 拦截器类
     * @param interceptorType 拦截器所属类型
     * @return
     */
    private TypeHandler createInterceptorTypeHandler(Class<? extends Interceptor> interceptorClazz, InterceptorType interceptorType) {
        if (interceptorClazz == null) {
            throw new NullPointerException("targetInterceptorClazz must not be null");
        }
        if (interceptorType == null) {
            throw new NullPointerException("interceptorType must not be null");
        }

        //获取方法
        final Method[] declaredMethods = interceptorClazz.getDeclaredMethods();
        //长度是2
        if (declaredMethods.length != 2) {
            throw new RuntimeException("invalid Type");
        }
        final String before = "before";
        //找到before方法
        final Method beforeMethod = findMethodByName(declaredMethods, before);
        //找到方法参数
        final Class<?>[] beforeParamList = beforeMethod.getParameterTypes();

        final String after = "after";
        //找到after方法
        final Method afterMethod = findMethodByName(declaredMethods, after);
        //找到方法参数
        final Class<?>[] afterParamList = afterMethod.getParameterTypes();
        //返回
        return new TypeHandler(interceptorClazz, interceptorType, before, beforeParamList, after, afterParamList);
    }


    private Method findMethodByName(Method[] declaredMethods, String methodName) {
        Method findMethod = null;
        int count = 0;
        for (Method method : declaredMethods) {
            if (method.getName().equals(methodName)) {
                count++;
                findMethod = method;
            }
        }
        if (findMethod == null) {
            throw new RuntimeException(methodName + " not found");
        }
        if (count > 1 ) {
            throw new RuntimeException("duplicated method exist. methodName:" + methodName);
        }
        return findMethod;
    }


    private class TypeHandler {
        private final Class<? extends Interceptor> interceptorClazz;
        private final InterceptorType interceptorType;
        private final String before;
        private final Class<?>[] beforeParamList;
        private final String after;
        private final Class<?>[] afterParamList;

        public TypeHandler(Class<? extends Interceptor> interceptorClazz, InterceptorType interceptorType, String before, final Class<?>[] beforeParamList, final String after, final Class<?>[] afterParamList) {
            if (interceptorClazz == null) {
                throw new NullPointerException("targetInterceptorClazz must not be null");
            }
            if (interceptorType == null) {
                throw new NullPointerException("interceptorType must not be null");
            }
            if (before == null) {
                throw new NullPointerException("before must not be null");
            }
            if (beforeParamList == null) {
                throw new NullPointerException("beforeParamList must not be null");
            }
            if (after == null) {
                throw new NullPointerException("after must not be null");
            }
            if (afterParamList == null) {
                throw new NullPointerException("afterParamList must not be null");
            }
            this.interceptorClazz = interceptorClazz;
            this.interceptorType = interceptorType;
            this.before = before;
            this.beforeParamList = beforeParamList;
            this.after = after;
            this.afterParamList = afterParamList;
        }


        public InterceptorDefinition resolveType(Class<?> targetClazz) {
            if(!this.interceptorClazz.isAssignableFrom(targetClazz)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            final Class<? extends Interceptor> casting = (Class<? extends Interceptor>) targetClazz;
            return createInterceptorDefinition(casting);
        }

        /**
         * 创建拦截器定义
         * @param targetInterceptorClazz
         * @return
         */
        private InterceptorDefinition createInterceptorDefinition(Class<? extends Interceptor> targetInterceptorClazz) {

            final Method beforeMethod = searchMethod(targetInterceptorClazz, before, beforeParamList);
            if (beforeMethod == null) {
                throw new RuntimeException(before + " method not found. " + Arrays.toString(beforeParamList));
            }
            final boolean beforeIgnoreMethod = beforeMethod.isAnnotationPresent(IgnoreMethod.class);


            final Method afterMethod = searchMethod(targetInterceptorClazz, after, afterParamList);
            if (afterMethod == null) {
                throw new RuntimeException(after + " method not found. " + Arrays.toString(afterParamList));
            }
            final boolean afterIgnoreMethod = afterMethod.isAnnotationPresent(IgnoreMethod.class);


            if (beforeIgnoreMethod == true && afterIgnoreMethod == true) {
                return new DefaultInterceptorDefinition(interceptorClazz, targetInterceptorClazz, interceptorType, CaptureType.NON, null, null);
            }
            if (beforeIgnoreMethod == true) {
                return new DefaultInterceptorDefinition(interceptorClazz, targetInterceptorClazz, interceptorType, CaptureType.AFTER, null, afterMethod);
            }
            if (afterIgnoreMethod == true) {
                return new DefaultInterceptorDefinition(interceptorClazz, targetInterceptorClazz, interceptorType, CaptureType.BEFORE, beforeMethod, null);
            }
            return new DefaultInterceptorDefinition(interceptorClazz, targetInterceptorClazz, interceptorType, CaptureType.AROUND, beforeMethod, afterMethod);
        }

        private Method searchMethod(Class<?> interceptorClazz, String searchMethodName, Class<?>[] searchMethodParameter) {
            if (searchMethodName == null) {
                throw new NullPointerException("methodList must not be null");
            }
//          only DeclaredMethod search ?
//            try {
//                return targetInterceptorClazz.getDeclaredMethod(searchMethodName, searchMethodParameter);
//            } catch (NoSuchMethodException ex) {
//                logger.debug(searchMethodName + " DeclaredMethod not found. search parent class");
//            }
            // search all class
            try {
                return interceptorClazz.getMethod(searchMethodName, searchMethodParameter);
            } catch (NoSuchMethodException ex) {
                logger.debug(searchMethodName +" DeclaredMethod not found.");
            }
            return null;
        }
    }



}
