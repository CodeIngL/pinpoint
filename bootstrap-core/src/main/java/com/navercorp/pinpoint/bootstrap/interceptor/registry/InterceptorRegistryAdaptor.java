package com.navercorp.pinpoint.bootstrap.interceptor.registry;

import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;


/**
 * 拦截器注册表适配器
 * @author emeroad
 */
public interface InterceptorRegistryAdaptor {
    int addInterceptor(Interceptor interceptor);
    Interceptor getInterceptor(int key);
}
