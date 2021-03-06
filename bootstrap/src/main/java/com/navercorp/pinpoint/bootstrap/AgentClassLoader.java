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

package com.navercorp.pinpoint.bootstrap;

import com.navercorp.pinpoint.bootstrap.classloader.PinpointClassLoaderFactory;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Callable;


/**
 * 代理类加载,
 * 持有一个自定义的classLoader
 * 加载lib中的相关jar
 * * @author emeroad
 */
public class AgentClassLoader {

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    private final URLClassLoader classLoader;

    private String bootClass;

    private final ContextClassLoaderExecuteTemplate<Object> executeTemplate;

    /**
     * 构造
     * @param urls
     */
    public AgentClassLoader(URL[] urls) {
        if (urls == null) {
            throw new NullPointerException("urls");
        }

        //获得本身这个类的类加载器
        ClassLoader bootStrapClassLoader = AgentClassLoader.class.getClassLoader();
        //构建类加载器中的自身加载器
        this.classLoader = createClassLoader(urls, bootStrapClassLoader);

        //构建模板,里面有asm等等包，用于应用asm时执行的模板
        this.executeTemplate = new ContextClassLoaderExecuteTemplate<Object>(classLoader);
    }

    private URLClassLoader createClassLoader(final URL[] urls, final ClassLoader bootStrapClassLoader) {
        if (SECURITY_MANAGER != null) {
            return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
                public URLClassLoader run() {
                    return PinpointClassLoaderFactory.createClassLoader(urls, bootStrapClassLoader);
                }
            });
        } else {
            return PinpointClassLoaderFactory.createClassLoader(urls, bootStrapClassLoader);
        }
    }

    public void setBootClass(String bootClass) {
        this.bootClass = bootClass;
    }

    public Agent boot(final AgentOption agentOption) {

        final Class<?> bootStrapClazz = getBootStrapClass();

        //默认com.navercorp.pinpoint.profiler.DefaultAgent，test：com.navercorp.pinpoint.test.PluginTestAgent
        //构建实例，调用构造函数
        final Object agent = executeTemplate.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Constructor<?> constructor = bootStrapClazz.getConstructor(AgentOption.class);
                    return constructor.newInstance(agentOption);
                } catch (InstantiationException e) {
                    throw new BootStrapException("boot create failed. Error:" + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    throw new BootStrapException("boot method invoke failed. Error:" + e.getMessage(), e);
                }
            }
        });

        if (agent instanceof Agent) {
            return (Agent) agent;
        } else {
            String agentClassName;
            if (agent == null) {
                agentClassName = "Agent is null";
            } else {
                agentClassName = agent.getClass().getName();
            }
            throw new BootStrapException("Invalid AgentType. boot failed. AgentClass:" + agentClassName);
        }
    }

    private Class<?> getBootStrapClass() {
        try {
            return this.classLoader.loadClass(bootClass);
        } catch (ClassNotFoundException e) {
            throw new BootStrapException("boot class not found. bootClass:" + bootClass + " Error:" + e.getMessage(), e);
        }
    }

}
