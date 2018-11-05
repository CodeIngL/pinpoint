package com.navercorp.pinpoint.bootstrap.interceptor.registry;

import java.util.concurrent.atomic.AtomicInteger;

import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.LoggingInterceptor;

/**
 * @author emeroad
 */
public final class DefaultInterceptorRegistryAdaptor implements InterceptorRegistryAdaptor {
    private static final LoggingInterceptor LOGGING_INTERCEPTOR = new LoggingInterceptor("com.navercorp.pinpoint.profiler.interceptor.LOGGING_INTERCEPTOR");

    private final static int DEFAULT_MAX = 8192;
    private final int registrySize;

    private final AtomicInteger id = new AtomicInteger(0);

    private final WeakAtomicReferenceArray<Interceptor> index;

    public DefaultInterceptorRegistryAdaptor() {
        this(DEFAULT_MAX);
    }

    /**
     * 构造函数，构造默认的拦截器注册表适配器
     * @param maxRegistrySize
     */
    public DefaultInterceptorRegistryAdaptor(int maxRegistrySize) {
        if (maxRegistrySize < 0) {
            throw new IllegalArgumentException("negative maxRegistrySize:" + maxRegistrySize);
        }
        this.registrySize = maxRegistrySize;
        this.index = new WeakAtomicReferenceArray<Interceptor>(maxRegistrySize, Interceptor.class);
    }


    /**
     * 添加拦截器
     * @param interceptor 拦截器
     * @return
     */
    @Override
    public int addInterceptor(Interceptor interceptor) {
        if (interceptor == null) {
            return -1;
        }

        //获得一个递增的id，用于标识拦截器
        final int newId = nextId();
        if (newId >= registrySize) {
            throw new IndexOutOfBoundsException("size=" + index.length() + " id=" + id);
        }
        index.set(newId, interceptor);
        return newId;
    }

    private int nextId() {
        return id.getAndIncrement();
    }

    /**
     * 根据拦截器Id
     * 获得拦截器
     *
     * 不存在则返回日志拦截器
     *
     * @param key
     * @return
     */
    public Interceptor getInterceptor(int key) {
        final Interceptor interceptor = this.index.get(key);
        if (interceptor == null) {
            return LOGGING_INTERCEPTOR;
        } else {
            return interceptor;
        }
    }
}
