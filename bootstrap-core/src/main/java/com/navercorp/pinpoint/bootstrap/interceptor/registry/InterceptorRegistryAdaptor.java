package com.navercorp.pinpoint.bootstrap.interceptor.registry;

import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;


/**
 * 拦截器注册表适配器
 * @author emeroad
 */
public interface InterceptorRegistryAdaptor {
    /**
     * 添加拦截器
     * @param interceptor
     * @return 拦截器id
     */
    int addInterceptor(Interceptor interceptor);

    /**
     * 获得对应的拦截器
     * @param key
     * @return
     */
    Interceptor getInterceptor(int key);
}
