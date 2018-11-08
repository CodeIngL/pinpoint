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

package com.navercorp.pinpoint.bootstrap.context;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;
import com.navercorp.pinpoint.common.annotations.InterfaceAudience;
import com.navercorp.pinpoint.common.annotations.InterfaceStability;

/**
 * @author emeroad
 * @author hyungil.jeong
 * @author Taejin Koo
 */
public interface TraceContext {

    /**
     * 从当前的上下文中返回一个经校验，需要采样的Trace
     * @return
     */
    Trace currentTraceObject();

    /**
     * return a trace whose sampling rate should be further verified
     * 返回一个Trace，其是否采样应进一步验证
     * @return
     */
    Trace currentRawTraceObject();

    /**
     * 根据一个TraceId，继续一个Trace，
     * 本质上讲这个TraceId中的Trace绑定到当前上下文
     * @param traceId
     * @return
     */
    Trace continueTraceObject(TraceId traceId);

    /**
     * 根据一个Trace，继续一个Trace，
     * 本质上讲这个Trace绑定当前上下文
     * @param trace
     * @return
     */
    Trace continueTraceObject(Trace trace);

    /**
     * 新建一个Trace
     * @return
     */
    Trace newTraceObject();

    /**
     * internal experimental api
     * 内部实验性API
     */
    @InterfaceStability.Evolving
    @InterfaceAudience.LimitedPrivate("vert.x")
    Trace newAsyncTraceObject();

    /**
     * internal experimental api
     * 内部实验性API
     */
    @InterfaceStability.Evolving
    @InterfaceAudience.LimitedPrivate("vert.x")
    Trace continueAsyncTraceObject(TraceId traceId);

    /**
     *
     * @deprecated Since 1.7.0
     */
    @Deprecated
    Trace continueAsyncTraceObject(AsyncTraceId traceId, int asyncId, long startTime);

    /**
     * 移除当前上下文的Trace，返回该Trace
     * @return
     */
    Trace removeTraceObject();

    /**
     *
     * 返回一个包装的被当前上下文移除的Trace，返回该Trace或者改包装的Trace，这取决于参数是否为True
     * @param closeDisableTrace true
     * @return
     * @since 1.7.0
     */
    Trace removeTraceObject(boolean closeDisableTrace);

    // ActiveThreadCounter getActiveThreadCounter();

    String getAgentId();

    String getApplicationName();

    long getAgentStartTime();

    short getServerTypeCode();

    String getServerType();

    int cacheApi(MethodDescriptor methodDescriptor);

    int cacheString(String value);

    // TODO extract jdbc related methods
    ParsingResult parseSql(String sql);

    boolean cacheSql(ParsingResult parsingResult);

    TraceId createTraceId(String transactionId, long parentSpanId, long spanId, short flags);

    Trace disableSampling();

    ProfilerConfig getProfilerConfig();

    ServerMetaDataHolder getServerMetaDataHolder();

    /**
     * internal api
     * @deprecated Since 1.7.0
     */
    @Deprecated
    int getAsyncId();

    JdbcContext getJdbcContext();

}
