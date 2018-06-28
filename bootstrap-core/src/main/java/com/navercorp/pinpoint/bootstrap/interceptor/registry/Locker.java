package com.navercorp.pinpoint.bootstrap.interceptor.registry;


/**
 * 锁实现，不可重入
 * @author emeroad
 */
public interface Locker {

    boolean lock(Object lock);

    boolean unlock(Object lock);

    Object getLock();
}
