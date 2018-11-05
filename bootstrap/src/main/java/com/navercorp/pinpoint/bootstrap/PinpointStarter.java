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
package com.navercorp.pinpoint.bootstrap;

import com.navercorp.pinpoint.ProductInfo;
import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.Version;
import com.navercorp.pinpoint.common.service.AnnotationKeyRegistryService;
import com.navercorp.pinpoint.common.service.DefaultAnnotationKeyRegistryService;
import com.navercorp.pinpoint.common.service.DefaultServiceTypeRegistryService;
import com.navercorp.pinpoint.common.service.DefaultTraceMetadataLoaderService;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.common.service.TraceMetadataLoaderService;
import com.navercorp.pinpoint.common.util.PinpointThreadFactory;
import com.navercorp.pinpoint.common.util.SimpleProperty;
import com.navercorp.pinpoint.common.util.SystemProperty;
import com.navercorp.pinpoint.common.util.logger.CommonLoggerFactory;
import com.navercorp.pinpoint.common.util.logger.StdoutCommonLoggerFactory;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jongho Moon
 *
 */
class PinpointStarter {

    private final BootLogger logger = BootLogger.getLogger(PinpointStarter.class.getName());

    public static final String AGENT_TYPE = "AGENT_TYPE";

    public static final String DEFAULT_AGENT = "DEFAULT_AGENT";
    public static final String BOOT_CLASS = "com.navercorp.pinpoint.profiler.DefaultAgent";

    public static final String PLUGIN_TEST_AGENT = "PLUGIN_TEST";
    public static final String PLUGIN_TEST_BOOT_CLASS = "com.navercorp.pinpoint.test.PluginTestAgent";

    private SimpleProperty systemProperty = SystemProperty.INSTANCE;

    //命令行参数
    private final Map<String, String> agentArgs;
    //boot目录下的jar
    private final BootstrapJarFile bootstrapJarFile;
    //解析
    private final ClassPathResolver classPathResolver;
    //Instrumentation
    private final Instrumentation instrumentation;


    public PinpointStarter(Map<String, String> agentArgs, BootstrapJarFile bootstrapJarFile, ClassPathResolver classPathResolver, Instrumentation instrumentation) {
        if (agentArgs == null) {
            throw new NullPointerException("agentArgs must not be null");
        }
        if (bootstrapJarFile == null) {
            throw new NullPointerException("bootstrapJarFile must not be null");
        }
        if (classPathResolver == null) {
            throw new NullPointerException("classPathResolver must not be null");
        }
        if (instrumentation == null) {
            throw new NullPointerException("instrumentation must not be null");
        }
        this.agentArgs = agentArgs;
        this.bootstrapJarFile = bootstrapJarFile;
        this.classPathResolver = classPathResolver;
        this.instrumentation = instrumentation;

    }

    /**
     * 开始函数，premian核心函数
     * @return
     */
    boolean start() {
        final IdValidator idValidator = new IdValidator();
        //agentID
        //命令行参数-Dpinpoint.agentId=xxx必填
        final String agentId = idValidator.getAgentId();
        if (agentId == null) {
            return false;
        }
        //agentName
        //命令行参数-Dpinpoint.applicationName=xxx必填
        final String applicationName = idValidator.getApplicationName();
        if (applicationName == null) {
            return false;
        }

        //加载插件URL在agent下的子目录plugin下
        URL[] pluginJars = classPathResolver.resolvePlugins();

        // TODO using PLogger instead of CommonLogger
        //日志，一般请忽略
        CommonLoggerFactory loggerFactory = StdoutCommonLoggerFactory.INSTANCE;

        //加载插件中的TraceMetadataProvider实现
        //追踪元数据的服务载体
        TraceMetadataLoaderService typeLoaderService = new DefaultTraceMetadataLoaderService(pluginJars, loggerFactory);
        //服务类型（插件类型）注册表服务的载体
        ServiceTypeRegistryService serviceTypeRegistryService = new DefaultServiceTypeRegistryService(typeLoaderService, loggerFactory);
        //注解键（插件类型）注册表服务的载体
        AnnotationKeyRegistryService annotationKeyRegistryService = new DefaultAnnotationKeyRegistryService(typeLoaderService, loggerFactory);


        //获得配置文件的路径，必须存在配置文件
        String configPath = getConfigPath(classPathResolver);
        if (configPath == null) {
            return false;
        }

        // set the path of log file as a system property
        // 设置日志路径到系统属性中，这里是一个目录
        saveLogFilePath(classPathResolver);

        //设置pinpoint的版本信息到系统属性中去
        savePinpointVersion();

        try {
            // Is it right to load the configuration in the bootstrap?
            // 在引导中加载配置是否正确？
            //默认的Profile实现，根据配置文件生成一些属性
            ProfilerConfig profilerConfig = DefaultProfilerConfig.load(configPath);

            // this is the library list that must be loaded
            // 加载lib文件，这些库是依赖项，必备的
            List<URL> libUrlList = resolveLib(classPathResolver);
            //构建agentClassLoader，这些lib中的jar准备由他加载
            AgentClassLoader agentClassLoader = new AgentClassLoader(libUrlList.toArray(new URL[libUrlList.size()]));
            //获得引导类
            //默认com.navercorp.pinpoint.profiler.DefaultAgent，test：com.navercorp.pinpoint.test.PluginTestAgent
            final String bootClass = getBootClass();
            //设置引导类
            agentClassLoader.setBootClass(bootClass);
            logger.info("pinpoint agent [" + bootClass + "] starting...");


            //创建agent选项
            AgentOption option = createAgentOption(agentId, applicationName, profilerConfig, instrumentation, pluginJars, bootstrapJarFile, serviceTypeRegistryService, annotationKeyRegistryService);

            //引导，将agent选项设置进引导类中“bootclass”对应的引导类中
            Agent pinpointAgent = agentClassLoader.boot(option);
            //agent开始
            pinpointAgent.start();
            //注册钩子
            registerShutdownHook(pinpointAgent);
            logger.info("pinpoint agent started normally.");
        } catch (Exception e) {
            // unexpected exception that did not be checked above
            logger.warn(ProductInfo.NAME + " start failed.", e);
            return false;
        }
        return true;
    }

    private String getBootClass() {
        final String agentType = getAgentType().toUpperCase();
        if (PLUGIN_TEST_AGENT.equals(agentType)) {
            return PLUGIN_TEST_BOOT_CLASS;
        }
        return BOOT_CLASS;
    }

    private String getAgentType() {
        String agentType = agentArgs.get(AGENT_TYPE);
        if (agentType == null) {
            return DEFAULT_AGENT;
        }
        return agentType;

    }

    /***
     *
     * @param agentId
     * @param applicationName
     * @param profilerConfig
     * @param instrumentation
     * @param pluginJars
     * @param bootstrapJarFile
     * @param serviceTypeRegistryService
     * @param annotationKeyRegistryService
     * @return
     */
    private AgentOption createAgentOption(String agentId, String applicationName, ProfilerConfig profilerConfig,
                                          Instrumentation instrumentation,
                                          URL[] pluginJars,
                                          BootstrapJarFile bootstrapJarFile,
                                          ServiceTypeRegistryService serviceTypeRegistryService,
                                          AnnotationKeyRegistryService annotationKeyRegistryService) {
        //获得jarFile的名字
        List<String> bootstrapJarPaths = bootstrapJarFile.getJarNameList();
        return new DefaultAgentOption(instrumentation, agentId, applicationName, profilerConfig, pluginJars, bootstrapJarPaths, serviceTypeRegistryService, annotationKeyRegistryService);
    }

    // for test
    void setSystemProperty(SimpleProperty systemProperty) {
        this.systemProperty = systemProperty;
    }

    private void registerShutdownHook(final Agent pinpointAgent) {
        final Runnable stop = new Runnable() {
            @Override
            public void run() {
                pinpointAgent.stop();
            }
        };
        PinpointThreadFactory pinpointThreadFactory = new PinpointThreadFactory("Pinpoint-shutdown-hook", false);
        Thread thread = pinpointThreadFactory.newThread(stop);
        Runtime.getRuntime().addShutdownHook(thread);
    }


    private void saveLogFilePath(ClassPathResolver classPathResolver) {
        String agentLogFilePath = classPathResolver.getAgentLogFilePath();
        logger.info("logPath:" + agentLogFilePath);

        systemProperty.setProperty(ProductInfo.NAME + ".log", agentLogFilePath);
    }

    private void savePinpointVersion() {
        logger.info("pinpoint version:" + Version.VERSION);
        systemProperty.setProperty(ProductInfo.NAME + ".version", Version.VERSION);
    }

    /**
     * 获得配置文件的路径
     * step 1: 从系统配置中获得配置文件的路径
     * step 2: 从agent的同级目录下的配置文件pinpoint.config
     * @param classPathResolver
     * @return
     */
    private String getConfigPath(ClassPathResolver classPathResolver) {
        final String configName = ProductInfo.NAME + ".config";
        String pinpointConfigFormSystemProperty = systemProperty.getProperty(configName);
        if (pinpointConfigFormSystemProperty != null) {
            logger.info(configName + " systemProperty found. " + pinpointConfigFormSystemProperty);
            return pinpointConfigFormSystemProperty;
        }

        String classPathAgentConfigPath = classPathResolver.getAgentConfigPath();
        if (classPathAgentConfigPath != null) {
            logger.info("classpath " + configName + " found. " + classPathAgentConfigPath);
            return classPathAgentConfigPath;
        }

        logger.info(configName + " file not found.");
        return null;
    }


    private List<URL> resolveLib(ClassPathResolver classPathResolver) {
        // this method may handle only absolute path,  need to handle relative path (./..agentlib/lib)
        String agentJarFullPath = classPathResolver.getAgentJarFullPath();
        String agentLibPath = classPathResolver.getAgentLibPath();
        List<URL> urlList = resolveLib(classPathResolver.resolveLib());
        String agentConfigPath = classPathResolver.getAgentConfigPath();

        if (logger.isInfoEnabled()) {
            logger.info("agent JarPath:" + agentJarFullPath);
            logger.info("agent LibDir:" + agentLibPath);
            for (URL url : urlList) {
                logger.info("agent Lib:" + url);
            }
            logger.info("agent config:" + agentConfigPath);
        }

        return urlList;
    }

    private List<URL> resolveLib(List<URL> urlList) {
        if (DEFAULT_AGENT.equals(getAgentType().toUpperCase())) {
            final List<URL> releaseLib = new ArrayList<URL>(urlList.size());
            for (URL url : urlList) {
                //
                if (!url.toExternalForm().contains("pinpoint-profiler-test")) {
                    releaseLib.add(url);
                }
            }
            return releaseLib;
        } else {
            logger.info("load " + PLUGIN_TEST_AGENT + " lib");
            // plugin test
            return urlList;
        }
    }

}
