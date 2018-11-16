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
package com.navercorp.pinpoint.profiler.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.profiler.instrument.InstrumentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.common.plugin.PluginLoader;
import com.navercorp.pinpoint.profiler.instrument.classloading.ClassInjector;
import com.navercorp.pinpoint.profiler.instrument.classloading.JarProfilerPluginClassInjector;

/**
 * @author Jongho Moon
 *
 */
public class ProfilerPluginLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ClassNameFilter profilerPackageFilter = new PinpointProfilerPackageSkipFilter();

    private final ProfilerConfig profilerConfig;
    private final PluginSetup pluginSetup;
    private final InstrumentEngine instrumentEngine;


    public ProfilerPluginLoader(ProfilerConfig profilerConfig, PluginSetup pluginSetup, InstrumentEngine instrumentEngine) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        if (pluginSetup == null) {
            throw new NullPointerException("pluginSetup must not be null");
        }
        if (instrumentEngine == null) {
            throw new NullPointerException("instrumentEngine must not be null");
        }
        this.profilerConfig = profilerConfig;
        this.pluginSetup = pluginSetup;
        this.instrumentEngine = instrumentEngine;
    }

    /**
     * 加载插件
     * @param pluginJars
     * @return
     */
    public List<SetupResult> load(URL[] pluginJars) {

        List<SetupResult> pluginContexts = new ArrayList<SetupResult>(pluginJars.length);

        for (URL pluginJar : pluginJars) {

            //加载jar
            final JarFile pluginJarFile = createJarFile(pluginJar);
            //获得jar中的各类包名
            final List<String> pluginPackageList = getPluginPackage(pluginJarFile);

            //创建过滤链
            final ClassNameFilter pluginFilterChain = createPluginFilterChain(pluginPackageList);

            //spi加载插件**ProfilerPlugin**
            final List<ProfilerPlugin> original = PluginLoader.load(ProfilerPlugin.class, new URL[] { pluginJar });

            //过滤掉被禁止的插件
            List<ProfilerPlugin> plugins = filterDisablePlugin(original);

            for (ProfilerPlugin plugin : plugins) {
                 if (logger.isInfoEnabled()) {
                    logger.info("{} Plugin {}:{}", plugin.getClass(), PluginConfig.PINPOINT_PLUGIN_PACKAGE, pluginPackageList);
                }
                
                logger.info("Loading plugin:{} pluginPackage:{}", plugin.getClass().getName(), plugin);

                //构建插件的配置对象
                PluginConfig pluginConfig = new PluginConfig(pluginJar, pluginFilterChain);
                //构建PluginClassInjector
                final ClassInjector classInjector = new JarProfilerPluginClassInjector(pluginConfig, instrumentEngine);
                //设置插件获得结果
                final SetupResult result = pluginSetup.setupPlugin(plugin, classInjector);
                //插件上下文中添加相关result
                pluginContexts.add(result);
            }
        }
        

        return pluginContexts;
    }

    /**
     * 进行过滤，过滤掉相关的被禁止的插件
     * @param plugins
     * @return
     */
    private List<ProfilerPlugin> filterDisablePlugin(List<ProfilerPlugin> plugins) {

        List<String> disabled = profilerConfig.getDisabledPlugins();

        List<ProfilerPlugin> result = new ArrayList<ProfilerPlugin>();
        for (ProfilerPlugin plugin : plugins) {
            if (disabled.contains(plugin.getClass().getName())) {
                logger.info("Skip disabled plugin: {}", plugin.getClass().getName());
                continue;
            }
            result.add(plugin);
        }
        return result;
    }

    /**
     * 构建类名过滤器
     * @param packageList
     * @return
     */
    private ClassNameFilter createPluginFilterChain(List<String> packageList) {

        //包名过滤器，该过滤器持有了该插件中的包名
        final ClassNameFilter pluginPackageFilter = new PluginPackageFilter(packageList);

        //pinpoint内置的过滤，构建统一的过滤链
        final List<ClassNameFilter> chain = Arrays.asList(profilerPackageFilter, pluginPackageFilter);

        final ClassNameFilter filterChain = new ClassNameFilterChain(chain);

        //返回组合
        return filterChain;
    }

    private JarFile createJarFile(URL pluginJar) {
        try {
            final URI uri = pluginJar.toURI();
            return new JarFile(new File(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException("URISyntax error. " + e.getCause(), e);
        } catch (IOException e) {
            throw new RuntimeException("IO error. " + e.getCause(), e);
        }
    }
    private Manifest getManifest(JarFile pluginJarFile) {
        try {
            return pluginJarFile.getManifest();
        } catch (IOException ex) {
            logger.info("{} IoError :{}", pluginJarFile.getName(), ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * 获得插件的包名
     * 从mainfest中获取
     * 不存在返回默认的包名
     * @param pluginJarFile
     * @return
     */
    public List<String> getPluginPackage(JarFile pluginJarFile) {

        final Manifest manifest =  getManifest(pluginJarFile);
        if (manifest == null) {
            return PluginConfig.DEFAULT_PINPOINT_PLUGIN_PACKAGE_NAME;
        }

        final Attributes attributes = manifest.getMainAttributes();
        final String pluginPackage = attributes.getValue(PluginConfig.PINPOINT_PLUGIN_PACKAGE);
        if (pluginPackage == null) {
            return PluginConfig.DEFAULT_PINPOINT_PLUGIN_PACKAGE_NAME;
        }
        return StringUtils.tokenizeToStringList(pluginPackage, ",");
    }


}
