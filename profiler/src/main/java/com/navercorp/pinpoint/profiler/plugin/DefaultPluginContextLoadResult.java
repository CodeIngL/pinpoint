/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.plugin;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.instrument.DynamicTransformTrigger;
import com.navercorp.pinpoint.profiler.instrument.InstrumentEngine;
import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcUrlParserV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认的插件上下文结果加载器
 * notice:这是一个整体插件，而不仅仅是单个插件
 * @author Woonduk Kang(emeroad)
 */
public class DefaultPluginContextLoadResult implements PluginContextLoadResult {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final URL[] pluginJars;
    private final InstrumentEngine instrumentEngine;

    private final ProfilerConfig profilerConfig;
    private final DynamicTransformTrigger dynamicTransformTrigger;

    private final List<SetupResult> setupResultList;

    /**
     * 默认的插件上下文结果加载器
     * @param profilerConfig
     * @param dynamicTransformTrigger
     * @param instrumentEngine
     * @param pluginJars
     */
    public DefaultPluginContextLoadResult(ProfilerConfig profilerConfig, DynamicTransformTrigger dynamicTransformTrigger, InstrumentEngine instrumentEngine,
                                               URL[] pluginJars) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        if (dynamicTransformTrigger == null) {
            throw new NullPointerException("dynamicTransformTrigger must not be null");
        }
        if (instrumentEngine == null) {
            throw new NullPointerException("instrumentEngine must not be null");
        }
        if (pluginJars == null) {
            throw new NullPointerException("pluginJars must not be null");
        }
        this.profilerConfig = profilerConfig;
        this.dynamicTransformTrigger = dynamicTransformTrigger;

        this.pluginJars = pluginJars;
        this.instrumentEngine = instrumentEngine;

        this.setupResultList = load();
    }


    /**
     * 开始加载 插件
     * @return
     */
    private List<SetupResult> load() {
        logger.info("load plugin");
        //构建插件设置器
        PluginSetup pluginSetup = new DefaultPluginSetup(profilerConfig, instrumentEngine, dynamicTransformTrigger);
        //构建插件加载器
        final ProfilerPluginLoader loader = new ProfilerPluginLoader(profilerConfig, pluginSetup, instrumentEngine);
        //使用加载器加载相应url下的插件，返回插件设置结果
        List<SetupResult> load = loader.load(pluginJars);
        return load;
    }

    /**
     * 获得所用插件的所有类文件转换器
     * 简单的遍历每个插件的设置结果，将每个插件得类文件转换器全部追加进
     * @return
     */
    @Override
    public List<ClassFileTransformer> getClassFileTransformer() {
        // TODO Need plugin context level grouping // TODO需要插件上下文级别分组
        final List<ClassFileTransformer> transformerList = new ArrayList<ClassFileTransformer>();
        for (SetupResult pluginContext : setupResultList) {
            List<ClassFileTransformer> classTransformerList = pluginContext.getClassTransformerList();
            transformerList.addAll(classTransformerList);
        }
        return transformerList;
    }


    /**
     * 获得所有插件的代表的应用类型
     * 简单的遍历每个插件的设置结果，将每个插件的的应用类型全部加入
     * @return
     */
    @Override
    public List<ApplicationTypeDetector> getApplicationTypeDetectorList() {
        final List<ApplicationTypeDetector> registeredDetectors = new ArrayList<ApplicationTypeDetector>();
        for (SetupResult context : setupResultList) {
            List<ApplicationTypeDetector> applicationTypeDetectors = context.getApplicationTypeDetectors();
            registeredDetectors.addAll(applicationTypeDetectors);
        }
        return registeredDetectors;
    }

    /**
     * 获得所用插件的对jdbcurl的解析
     * 简单的变量每个插件的设置结果，将每个插件的jdbc解析方式全部加入
     * @return
     */
    @Override
    public List<JdbcUrlParserV2> getJdbcUrlParserList() {
        final List<JdbcUrlParserV2> result = new ArrayList<JdbcUrlParserV2>();
        for (SetupResult context : setupResultList) {
            List<JdbcUrlParserV2> jdbcUrlParserList = context.getJdbcUrlParserList();
            result.addAll(jdbcUrlParserList);
        }
        return result;
    }

}
