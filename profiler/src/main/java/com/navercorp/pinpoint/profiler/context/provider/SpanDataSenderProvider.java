/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.context.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.profiler.context.module.SpanStatClientFactory;
import com.navercorp.pinpoint.profiler.sender.DataSender;
import com.navercorp.pinpoint.profiler.sender.TcpDataSender;
import com.navercorp.pinpoint.profiler.sender.UdpDataSenderFactory;
import com.navercorp.pinpoint.rpc.client.PinpointClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Span数据发送器提供者
 * @author Taejin Koo
 */
public class SpanDataSenderProvider  implements Provider<DataSender> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String UDP_EXECUTOR_NAME = "Pinpoint-UdpSpanDataExecutor";

    private final Provider<PinpointClientFactory> clientFactoryProvider;

    private final String ip;
    private final int port;
    private final int writeQueueSize;
    private final int timeout;
    private final int sendBufferSize;
    private final String ioType;
    private final String transportType;


    /**
     * 通过profiler的配置项，配置相关数据通过PinpointClient进行发送数据
     * 向collector组件发送span的相关信息
     * @param profilerConfig
     * @param clientFactoryProvider
     */
    @Inject
    public SpanDataSenderProvider(ProfilerConfig profilerConfig, @SpanStatClientFactory Provider<PinpointClientFactory> clientFactoryProvider) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        if (clientFactoryProvider == null) {
            throw new NullPointerException("clientFactoryProvider must not be null");
        }

        this.clientFactoryProvider = clientFactoryProvider;

        //collector组件的ip
        this.ip = profilerConfig.getCollectorSpanServerIp();
        //collector组件的port
        this.port = profilerConfig.getCollectorSpanServerPort();
        //写队列的大小
        this.writeQueueSize = profilerConfig.getSpanDataSenderWriteQueueSize();
        //socket超时事件
        this.timeout = profilerConfig.getSpanDataSenderSocketTimeout();
        //socket缓存区大小
        this.sendBufferSize = profilerConfig.getSpanDataSenderSocketSendBufferSize();
        //socket类型BIO or NIO
        this.ioType = profilerConfig.getSpanDataSenderSocketType();
        //传输类型 TCP or UDP
        this.transportType = profilerConfig.getSpanDataSenderTransportType();
    }

    @Override
    public DataSender get() {
        if ("TCP".equalsIgnoreCase(transportType)) {
            if ("OIO".equalsIgnoreCase(ioType)) {
                logger.warn("TCP transport not support OIO type.(only support NIO)");
            }
            PinpointClientFactory pinpointClientFactory = clientFactoryProvider.get();
            return new TcpDataSender("SpanDataSender", ip, port, pinpointClientFactory);
        } else {
            UdpDataSenderFactory factory = new UdpDataSenderFactory(ip, port, UDP_EXECUTOR_NAME, writeQueueSize, timeout, sendBufferSize);
            return factory.create(ioType);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpanDataSenderProvider{");
        sb.append("ip='").append(ip).append('\'');
        sb.append(", port=").append(port);
        sb.append(", writeQueueSize=").append(writeQueueSize);
        sb.append(", timeout=").append(timeout);
        sb.append(", sendBufferSize=").append(sendBufferSize);
        sb.append(", ioType='").append(ioType).append('\'');
        sb.append(", transportType='").append(transportType).append('\'');
        sb.append('}');
        return sb.toString();
    }

}

