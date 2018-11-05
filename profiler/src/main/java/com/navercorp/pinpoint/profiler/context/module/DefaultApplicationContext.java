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

package com.navercorp.pinpoint.profiler.context.module;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.navercorp.pinpoint.bootstrap.AgentOption;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.DynamicTransformTrigger;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.AgentInfoSender;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.ClassFileTransformerDispatcher;
import com.navercorp.pinpoint.profiler.context.ServerMetaDataRegistryService;
import com.navercorp.pinpoint.profiler.instrument.ASMBytecodeDumpService;
import com.navercorp.pinpoint.profiler.instrument.BytecodeDumpTransformer;
import com.navercorp.pinpoint.profiler.instrument.InstrumentEngine;
import com.navercorp.pinpoint.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.navercorp.pinpoint.profiler.monitor.AgentStatMonitor;
import com.navercorp.pinpoint.profiler.monitor.DeadlockMonitor;
import com.navercorp.pinpoint.profiler.sender.DataSender;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.rpc.client.PinpointClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

/**
 * 默认的对象容器的实现
 * @author Woonduk Kang(emeroad)
 */
public class DefaultApplicationContext implements ApplicationContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ProfilerConfig profilerConfig;

    private final DeadlockMonitor deadlockMonitor;
    private final AgentInfoSender agentInfoSender;
    private final AgentStatMonitor agentStatMonitor;

    private final TraceContext traceContext;

    private final PinpointClientFactory clientFactory;
    private final EnhancedDataSender tcpDataSender;

    private final PinpointClientFactory spanStatClientFactory;
    private final DataSender spanDataSender;
    private final DataSender statDataSender;

    private final AgentInformation agentInformation;
    private final ServerMetaDataRegistryService serverMetaDataRegistryService;

    private final ServiceTypeRegistryService serviceTypeRegistryService;

    private final ClassFileTransformerDispatcher classFileDispatcher;

    //系统的Instrumentation，由java自带
    private final Instrumentation instrumentation;
    //pinpoint中针对系统的Instrumentation的引擎，默认使用ASM
    private final InstrumentEngine instrumentEngine;
    //动态的Transform转换表
    private final DynamicTransformTrigger dynamicTransformTrigger;

    private final Injector injector;

    /**
     * 构建依赖容器
     * @param agentOption
     * @param interceptorRegistryBinder
     * @param moduleFactoryProvider
     */
    public DefaultApplicationContext(AgentOption agentOption, final InterceptorRegistryBinder interceptorRegistryBinder, ModuleFactoryProvider moduleFactoryProvider) {
        this(agentOption, interceptorRegistryBinder, moduleFactoryProvider.get());
    }

    /**
     * 构造函数
     * @param agentOption
     * @param interceptorRegistryBinder
     * @param moduleFactory
     */
    public DefaultApplicationContext(AgentOption agentOption, final InterceptorRegistryBinder interceptorRegistryBinder, ModuleFactory moduleFactory) {
        Assert.requireNonNull(agentOption, "agentOption must not be null");
        //绑定配置
        this.profilerConfig = Assert.requireNonNull(agentOption.getProfilerConfig(), "profilerConfig must not be null");
        Assert.requireNonNull(moduleFactory, "moduleFactory must not be null");

        //绑定instrumentation
        this.instrumentation = agentOption.getInstrumentation();
        //绑定serviceTypeRegistryService
        this.serviceTypeRegistryService = agentOption.getServiceTypeRegistryService();

        if (logger.isInfoEnabled()) {
            logger.info("DefaultAgent classLoader:{}", this.getClass().getClassLoader());
        }

        //模块配置，回调configura
        final Module applicationContextModule = moduleFactory.newModule(agentOption, interceptorRegistryBinder);
        //获得注入器
        this.injector = Guice.createInjector(Stage.PRODUCTION, applicationContextModule);

        //asm引擎
        this.instrumentEngine = injector.getInstance(InstrumentEngine.class);

        //默认的class转换分发器
        this.classFileDispatcher = injector.getInstance(ClassFileTransformerDispatcher.class);
        //动态的Transform注册表
        this.dynamicTransformTrigger = injector.getInstance(DynamicTransformTrigger.class);
//        ClassFileTransformer classFileTransformer = injector.getInstance(ClassFileTransformer.class);
        //wrap,简单的进行包装，用于某些情况下dump文件
        ClassFileTransformer classFileTransformer = wrap(classFileDispatcher);
        //追加进去
        instrumentation.addTransformer(classFileTransformer, true);

        this.spanStatClientFactory = injector.getInstance(Key.get(PinpointClientFactory.class, SpanStatClientFactory.class));
        logger.info("spanStatClientFactory:{}", spanStatClientFactory);

        //Udp的Span发送器
        this.spanDataSender = newUdpSpanDataSender();
        logger.info("spanDataSender:{}", spanDataSender);

        //Udp的状态发送器
        this.statDataSender = newUdpStatDataSender();
        logger.info("statDataSender:{}", statDataSender);

        //默认的客户端工厂
        this.clientFactory = injector.getInstance(Key.get(PinpointClientFactory.class, DefaultClientFactory.class));
        logger.info("clientFactory:{}", clientFactory);

        //tcp数据发送器
        this.tcpDataSender = injector.getInstance(EnhancedDataSender.class);
        logger.info("tcpDataSender:{}", tcpDataSender);

        //Trace上下文
        this.traceContext = injector.getInstance(TraceContext.class);

        //代理信息
        this.agentInformation = injector.getInstance(AgentInformation.class);
        logger.info("agentInformation:{}", agentInformation);
        //服务元数据注册表服务
        this.serverMetaDataRegistryService = injector.getInstance(ServerMetaDataRegistryService.class);

        //死锁监控
        this.deadlockMonitor = injector.getInstance(DeadlockMonitor.class);
        //代理信息发送者
        this.agentInfoSender = injector.getInstance(AgentInfoSender.class);
        //代理状态监控
        this.agentStatMonitor = injector.getInstance(AgentStatMonitor.class);
    }

    /**
     * 包装classFileTransformerDispatcher 获得一个的ClassFileTransformer
     * 存在bytecode.dump.enable dump出来
     * @param classFileTransformerDispatcher
     * @return
     */
    public ClassFileTransformer wrap(ClassFileTransformerDispatcher classFileTransformerDispatcher) {
        final boolean enableBytecodeDump = profilerConfig.readBoolean(ASMBytecodeDumpService.ENABLE_BYTECODE_DUMP, ASMBytecodeDumpService.ENABLE_BYTECODE_DUMP_DEFAULT_VALUE);
        if (enableBytecodeDump) {
            logger.info("wrapBytecodeDumpTransformer");
            return BytecodeDumpTransformer.wrap(classFileTransformerDispatcher, profilerConfig);
        }
        return classFileTransformerDispatcher;
    }

    protected Module newApplicationContextModule(AgentOption agentOption, InterceptorRegistryBinder interceptorRegistryBinder) {
        return new ApplicationContextModule(agentOption, interceptorRegistryBinder);
    }

    private DataSender newUdpStatDataSender() {
        Key<DataSender> statDataSenderKey = Key.get(DataSender.class, StatDataSender.class);
        return injector.getInstance(statDataSenderKey);
    }

    private DataSender newUdpSpanDataSender() {
        Key<DataSender> spanDataSenderKey = Key.get(DataSender.class, SpanDataSender.class);
        return injector.getInstance(spanDataSenderKey);
    }

    @Override
    public ProfilerConfig getProfilerConfig() {
        return profilerConfig;
    }

    public Injector getInjector() {
        return injector;
    }

    @Override
    public TraceContext getTraceContext() {
        return traceContext;
    }

    public DataSender getSpanDataSender() {
        return spanDataSender;
    }

    public InstrumentEngine getInstrumentEngine() {
        return instrumentEngine;
    }


    @Override
    public DynamicTransformTrigger getDynamicTransformTrigger() {
        return dynamicTransformTrigger;
    }


    @Override
    public ClassFileTransformerDispatcher getClassFileTransformerDispatcher() {
        return classFileDispatcher;
    }

    @Override
    public AgentInformation getAgentInformation() {
        return this.agentInformation;
    }

    public ServerMetaDataRegistryService getServerMetaDataRegistryService() {
        return this.serverMetaDataRegistryService;
    }

    /**
     * 死锁监控
     * 代理信息发送
     * 代理状态监控
     */
    @Override
    public void start() {
        this.deadlockMonitor.start();
        this.agentInfoSender.start();
        this.agentStatMonitor.start();
    }

    /**
     * 关闭操
     */
    @Override
    public void close() {
        //关闭一下信息
        this.agentInfoSender.stop();
        this.agentStatMonitor.stop();
        this.deadlockMonitor.stop();

        // Need to process stop
        // 需要处理stop
        this.spanDataSender.stop();
        this.statDataSender.stop();
        if (spanStatClientFactory != null) {
            spanStatClientFactory.release();
        }

        closeTcpDataSender();
    }

    /**
     * 关闭Tcp发送
     */
    private void closeTcpDataSender() {
        final EnhancedDataSender tcpDataSender = this.tcpDataSender;
        if (tcpDataSender != null) {
            tcpDataSender.stop();
        }
        final PinpointClientFactory clientFactory = this.clientFactory;
        if (clientFactory != null) {
            clientFactory.release();
        }
    }

}
