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

import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcUrlParserV2;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;

/**
 * 设置结果
 * 每一个插件都将对应一个设置结果
 * @author Woonduk Kang(emeroad)
 */
public class SetupResult {

    //设置上下文，插件
    private final DefaultProfilerPluginSetupContext setupContext;
    //类文件转换器，插件
    private final ClassFileTransformerLoader transformerRegistry;

    public SetupResult(DefaultProfilerPluginSetupContext setupContext, ClassFileTransformerLoader transformerRegistry) {
        this.setupContext = setupContext;
        this.transformerRegistry = transformerRegistry;
    }


    /**
     * 从设置上下文中，或的设置的应用类型
     * @return 列表
     */
    public List<ApplicationTypeDetector> getApplicationTypeDetectors() {
        return this.setupContext.getApplicationTypeDetectors();
    }

    /**
     * 从设置上下文中，获得设置的jdbcURL解析器
     * @return 列表
     */
    public List<JdbcUrlParserV2> getJdbcUrlParserList() {
        return this.setupContext.getJdbcUrlParserList();
    }

    /**
     * 获得该设置需要的相关的类文件转换器
     * @return 列表
     */
    public List<ClassFileTransformer> getClassTransformerList() {
        return transformerRegistry.getClassTransformerList();
    }


}
