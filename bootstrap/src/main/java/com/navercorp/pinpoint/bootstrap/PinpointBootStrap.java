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

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import com.navercorp.pinpoint.ProductInfo;

/**
 * agent的入口类
 * @see #premain(String, Instrumentation)
 * @author emeroad
 * @author netspider
 */
public class PinpointBootStrap {

    private static final BootLogger logger = BootLogger.getLogger(PinpointBootStrap.class.getName());

    private static final LoadState STATE = new LoadState();


    /**
     * java agent
     * @param agentArgs
     * @param instrumentation
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        if (agentArgs == null) {
            agentArgs = "";
        }
        logger.info(ProductInfo.NAME + " agentArgs:" + agentArgs);

        //stat状态设置
        final boolean success = STATE.start();
        if (!success) {
            logger.warn("pinpoint-bootstrap already started. skipping agent loading.");
            return;
        }
        //命令行参数转键值对
        //ex、agentArgs is "name=123,password=456"
        //the result is {"name":"123","password":"456"}
        Map<String, String> agentArgsMap = argsToMap(agentArgs);

        //使用系统路径构建类路径解析器
        final ClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver();
        //boot-strap-xxx.jar必有
        // boot路径下
        //其他pinpoint-commons.jar。bootstrap-core.jar必有
        // bootstrap-core-optional.jar,annotations.jar可选
        if (!classPathResolver.verify()) {
            logger.warn("Agent Directory Verify fail. skipping agent loading.");
            logPinpointAgentLoadFail();
            return;
        }

        BootstrapJarFile bootstrapJarFile = classPathResolver.getBootstrapJarFile();
        //添加进bootstrap
        appendToBootstrapClassLoader(instrumentation, bootstrapJarFile);


        //委托给starter起来
        //all 参数 非空
        PinpointStarter bootStrap = new PinpointStarter(agentArgsMap, bootstrapJarFile, classPathResolver, instrumentation);
        if (!bootStrap.start()) {
            logPinpointAgentLoadFail();
        }

    }

    /**
     * ex、agentArgs is "name=123,password=456"
     * the result is {"name":"123","password":"456"}
     * @param agentArgs
     * @return
     */
    private static Map<String, String> argsToMap(String agentArgs) {
        ArgsParser argsParser = new ArgsParser();
        Map<String, String> agentArgsMap = argsParser.parse(agentArgs);
        if (!agentArgsMap.isEmpty()) {
            logger.info("agentParameter :" + agentArgs);
        }
        return agentArgsMap;
    }

    /**
     * 追加进bootstrapClassLoader
     * @param instrumentation
     * @param agentJarFile
     */
    private static void appendToBootstrapClassLoader(Instrumentation instrumentation, BootstrapJarFile agentJarFile) {
        List<JarFile> jarFileList = agentJarFile.getJarFileList();
        for (JarFile jarFile : jarFileList) {
            logger.info("appendToBootstrapClassLoader:" + jarFile.getName());
            instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
        }
    }


    /**
     * failed logo print
     */
    private static void logPinpointAgentLoadFail() {
        final String errorLog =
                "*****************************************************************************\n" +
                        "* Pinpoint Agent load failure\n" +
                        "*****************************************************************************";
        System.err.println(errorLog);
    }


}
