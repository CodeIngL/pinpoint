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
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplateAware;
import com.navercorp.pinpoint.profiler.instrument.GuardInstrumentContext;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentContext;
import com.navercorp.pinpoint.profiler.instrument.InstrumentEngine;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.profiler.instrument.classloading.ClassInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认的插件设置器
 * @author Woonduk Kang(emeroad)
 */
public class DefaultPluginSetup implements PluginSetup {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ProfilerConfig profilerConfig;
    private final InstrumentEngine instrumentEngine;
    private final DynamicTransformTrigger dynamicTransformTrigger;


    public DefaultPluginSetup(ProfilerConfig profilerConfig, InstrumentEngine instrumentEngine, DynamicTransformTrigger dynamicTransformTrigger) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        if (instrumentEngine == null) {
            throw new NullPointerException("instrumentEngine must not be null");
        }
        if (dynamicTransformTrigger == null) {
            throw new NullPointerException("dynamicTransformTrigger must not be null");
        }
        this.profilerConfig = profilerConfig;
        this.instrumentEngine = instrumentEngine;
        this.dynamicTransformTrigger = dynamicTransformTrigger;
    }

    /**
     * 设置插件
     * @param profilerPlugin
     * @param classInjector
     * @return
     */
    @Override
    public SetupResult setupPlugin(ProfilerPlugin profilerPlugin, ClassInjector classInjector) {

        //类转换加载器
        final ClassFileTransformerLoader transformerRegistry = new ClassFileTransformerLoader(profilerConfig, dynamicTransformTrigger);
        //插件启动上下文
        final DefaultProfilerPluginSetupContext setupContext = new DefaultProfilerPluginSetupContext(profilerConfig);
        //插件保护上下文
        final GuardProfilerPluginContext guardSetupContext = new GuardProfilerPluginContext(setupContext);

        //检测上下文
        final InstrumentContext instrumentContext = new PluginInstrumentContext(profilerConfig, instrumentEngine, dynamicTransformTrigger, classInjector, transformerRegistry );
        //检测保护上下文，plugin准备，设置TransformTemplate
        final GuardInstrumentContext guardInstrumentContext = preparePlugin(profilerPlugin, instrumentContext);
        try {
            // WARN external plugin api
            if (logger.isInfoEnabled()) {
                logger.info("{} Plugin setup", profilerPlugin.getClass().getName());
            }
            //为插件设置插件保护上下文，只能这里进行设置，守护检测上下文在这之后会失效，原因在于finally的释放
            profilerPlugin.setup(guardSetupContext);
        } finally {
            guardSetupContext.close();
            guardInstrumentContext.close();
        }
        //返回设置结果
        SetupResult setupResult = new SetupResult(setupContext, transformerRegistry);
        return setupResult;
    }

    /**
     * 准备加载plugin
     * @param plugin
     * @param instrumentContext
     * @return
     */
    private GuardInstrumentContext preparePlugin(ProfilerPlugin plugin, InstrumentContext instrumentContext) {

        //构建守护的检测上下文，
        final GuardInstrumentContext guardInstrumentContext = new GuardInstrumentContext(instrumentContext);
        //为插件设置对应的TransformTemplate，所有的TransformTemplate都持有了守护的检测上下文。
        if (plugin instanceof TransformTemplateAware) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}.setTransformTemplate", plugin.getClass().getName());
            }
            final TransformTemplate transformTemplate = new TransformTemplate(guardInstrumentContext);
            ((TransformTemplateAware) plugin).setTransformTemplate(transformTemplate);
        } else if(plugin instanceof MatchableTransformTemplateAware) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}.setTransformTemplate", plugin.getClass().getName());
            }
            final MatchableTransformTemplate transformTemplate = new MatchableTransformTemplate(guardInstrumentContext);
            ((MatchableTransformTemplateAware) plugin).setTransformTemplate(transformTemplate);
        }
        return guardInstrumentContext;
    }

}
