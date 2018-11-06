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
     * 典型的tomcat的启动方式：
     * xxxxxBootstrap start
     * @param agentArgs 命令行参数 start就是启动参数
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
        //参数转键值对
        //"name=123,password=456" ===> {"name":"123","password":"456"}
        Map<String, String> agentArgsMap = argsToMap(agentArgs);

        //agent的路径解析解析器，agent位于一个单独的目录中，但本身符号javaagent规范的pinpoint的agent的jar会被引用在
        //System.getProperty("java.class.path");中
        final ClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver();
        if (!classPathResolver.verify()) {
            logger.warn("Agent Directory Verify fail. skipping agent loading.");
            logPinpointAgentLoadFail();
            return;
        }

        //boot目录下jar集合
        BootstrapJarFile bootstrapJarFile = classPathResolver.getBootstrapJarFile();
        //添加进bootstrap类引导类加载器的检测路径中
        appendToBootstrapClassLoader(instrumentation, bootstrapJarFile);


        //委托给starter起来
        PinpointStarter bootStrap = new PinpointStarter(agentArgsMap, bootstrapJarFile, classPathResolver, instrumentation);
        //启动
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
            //添加进bootstrapclassloader的类加载器中
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
