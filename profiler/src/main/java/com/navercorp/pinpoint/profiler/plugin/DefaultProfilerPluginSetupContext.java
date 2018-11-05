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

package com.navercorp.pinpoint.profiler.plugin;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcUrlParserV2;

import java.util.ArrayList;
import java.util.List;


/**
 * 默认的插件设置器的上下文，用于检测
 * @author jaehong.kim
 */
public class DefaultProfilerPluginSetupContext implements ProfilerPluginSetupContext {

    //全局配置
    private final ProfilerConfig profilerConfig;

    //应用检测列表，检测当前的服务的所属的应用类型，比如dubbo，http等等
    private final List<ApplicationTypeDetector> serverTypeDetectors = new ArrayList<ApplicationTypeDetector>();
    //jdbc解析器
    private final List<JdbcUrlParserV2> jdbcUrlParserList = new ArrayList<JdbcUrlParserV2>();

    public DefaultProfilerPluginSetupContext(ProfilerConfig profilerConfig) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }

        this.profilerConfig = profilerConfig;
    }

    @Override
    public ProfilerConfig getConfig() {
        return profilerConfig;
    }


    @Override
    public void addApplicationTypeDetector(ApplicationTypeDetector... detectors) {
        if (detectors == null) {
            return;
        }
        for (ApplicationTypeDetector detector : detectors) {
            serverTypeDetectors.add(detector);
        }
    }

    public List<ApplicationTypeDetector> getApplicationTypeDetectors() {
        return serverTypeDetectors;
    }

    @Override
    public void addJdbcUrlParser(JdbcUrlParserV2 jdbcUrlParser) {
        if (jdbcUrlParser == null) {
            return;
        }

        this.jdbcUrlParserList.add(jdbcUrlParser);
    }

    public List<JdbcUrlParserV2> getJdbcUrlParserList() {
        return jdbcUrlParserList;
    }

}
