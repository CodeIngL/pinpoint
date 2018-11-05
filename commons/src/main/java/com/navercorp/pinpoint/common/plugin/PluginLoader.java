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

package com.navercorp.pinpoint.common.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * TODO Loading all plugins with a single class loader could cause class collisions.
 *      Also, with current implementation, plugins can use dependencies by putting them in the plugin directory too.
 *      But it can lead to dependency collision between plugins because they are loaded by a single class loader.
 *      
 *      How can we prevent this?
 *      A ClassLoader per plugin could do it but then we have to create "N of target class loader" x "N of plugin" class loaders.
 *      It seems too much. For now, Just leave it as it is. 
 * 
 * 
 * @author Jongho Moon <jongho.moon@navercorp.com>
 * @author emeroad
 *
 * @param <T>
 */
public class PluginLoader {
    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    /**
     * 加载插件，插件应该独立与应用程序，因此这个为这些插件使用了单独的的类加载器
     * @param serviceType
     * @param urls
     * @param <T>
     * @return
     */
    public static <T> List<T> load(Class<T> serviceType, URL[] urls) {
        URLClassLoader classLoader = createPluginClassLoader(urls, ClassLoader.getSystemClassLoader());
        return load(serviceType, classLoader);
    }

    /**
     * 为插件创建单独的类加载器，用于加载插件
     * @param urls 插件的URL
     * @param parent 父加载器
     * @return 插件类加载器
     */
    private static PluginLoaderClassLoader createPluginClassLoader(final URL[] urls, final ClassLoader parent) {
        if (SECURITY_MANAGER != null) {
            return AccessController.doPrivileged(new PrivilegedAction<PluginLoaderClassLoader>() {
                public PluginLoaderClassLoader run() {
                    return new PluginLoaderClassLoader(urls, parent);
                }
            });
        } else {
            return new PluginLoaderClassLoader(urls, parent);
        }
    }

    /**
     * 加载插件等jar的相关类，这里使用java的SPI方式进行加载
     * @param serviceType
     * @param classLoader
     * @param <T>
     * @return
     */
    public static <T> List<T> load(Class<T> serviceType, ClassLoader classLoader) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(serviceType, classLoader);
        
        List<T> plugins = new ArrayList<T>();
        for (T plugin : serviceLoader) {
            plugins.add(serviceType.cast(plugin));
        }

        return plugins;
    }
}
