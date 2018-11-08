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

package com.navercorp.pinpoint.profiler.context.id;

import com.google.inject.Inject;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.common.util.BytesUtils;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.profiler.context.module.AgentId;
import com.navercorp.pinpoint.profiler.context.module.AgentStartTime;

import java.nio.ByteBuffer;

/**
 * @author Woonduk Kang(emeroad)
 */
public class DefaultTransactionIdEncoder implements TransactionIdEncoder {

    private static final byte VERSION = TransactionIdUtils.VERSION;

    //应用id
    private final String agentId;
    //应用启动时间
    private final long agentStartTime;

    //agentId缓存
    private final byte[] agentIdCache;
    //应用开始时间缓存
    private final byte[] agentIdAndStartTimeCache;

    @Inject
    public DefaultTransactionIdEncoder(@AgentId String agentId, @AgentStartTime  long agentStartTime) {
        this.agentId = Assert.requireNonNull(agentId, "agentId must not be null");
        this.agentStartTime = agentStartTime;

        this.agentIdCache = newCache(null);
        this.agentIdAndStartTimeCache= newCache(null,  agentStartTime);
    }

    private byte[] newCache(String agentId, long agentStartTime) {
        final int agentStartTimeSize = BytesUtils.computeVar64Size(agentStartTime);
        final int agentIdSize = StringUtils.getLength(agentId);
        final Buffer buffer = new AutomaticBuffer(1 + 5 + agentIdSize + agentStartTimeSize);
        buffer.putByte(VERSION);
        buffer.putPrefixedString(agentId);
        buffer.putVLong(agentStartTime);
        return buffer.copyBuffer();
    }

    private byte[] newCache(String agentId) {
        final int agentIdSize = StringUtils.getLength(agentId);
        final Buffer buffer = new AutomaticBuffer(1 + 5 + agentIdSize);
        buffer.putByte(VERSION);
        buffer.putPrefixedString(agentId);
        return buffer.copyBuffer();
    }


    private byte[] encode(String agentId, long agentStartTime, long transactionSequence) {
        return TransactionIdUtils.formatBytes(agentId, agentStartTime, transactionSequence);
    }

    private byte[] encodeAgentIdAndTransactionSequence(byte[] agentIdCache, long agentStartTime, long transactionSequence) {
        final int agentStartTimeSize = BytesUtils.computeVar64Size(agentStartTime);
        final int transactionSequenceSize = BytesUtils.computeVar64Size(transactionSequence);
        final int prefixLength = agentIdCache.length;
        final byte[] transactionId = new byte[prefixLength + agentStartTimeSize + transactionSequenceSize];
        // copy prefix
        System.arraycopy(agentIdCache, 0, transactionId, 0, prefixLength);

        BytesUtils.writeVar64(agentStartTime, transactionId, prefixLength);
        BytesUtils.writeVar64(transactionSequence, transactionId, prefixLength + agentStartTimeSize);

        return transactionId;
    }

    private byte[] encodeTransactionSequence(byte[] agentIdAndStartTimeCache, long transactionSequence) {
        final int transactionSequenceSize = BytesUtils.computeVar64Size(transactionSequence);
        final int prefixLength = agentIdAndStartTimeCache.length;
        final byte[] transactionId = new byte[prefixLength + transactionSequenceSize];
        // copy prefix
        System.arraycopy(agentIdAndStartTimeCache, 0, transactionId, 0, prefixLength);

        BytesUtils.writeVar64(transactionSequence, transactionId, prefixLength);

        return transactionId;
    }

    /**
     * skip agentId + agentStartTime
     * 跳过 agentId + agentStartTime
     */
    private byte[] encode(long transactionSequence) {
        final byte[] encode = encodeTransactionSequence(agentIdAndStartTimeCache, transactionSequence);
        return encode;
    }

    /**
     * 跳过 agentId
     * @param agentStartTime
     * @param transactionSequence
     * @return
     */
    private  byte[] encode(long agentStartTime, long transactionSequence) {
        final byte[] encode = encodeAgentIdAndTransactionSequence(agentIdCache, agentStartTime, transactionSequence);
        return encode;
    }

    /**
     * 是否压缩的，如果TraceId在应用内部，说明则不需要发送agentId这个东西了
     * @param traceId
     * @return
     */
    private boolean isCompressedType(TraceId traceId) {
        // skip agentId
        return agentId.equals(traceId.getAgentId());
    }

    /**
     * 编码一个TraceId
     * @param traceId
     * @return
     */
    @Override
    public ByteBuffer encodeTransactionId(TraceId traceId) {
        if (traceId == null) {
            throw new NullPointerException("traceId must not be null");
        }

        return ByteBuffer.wrap(encodeTransaction0(traceId));
    }

    private byte[] encodeTransaction0(TraceId traceId) {
        if (isCompressedType(traceId)) {
            //获得序列
            final long transactionSequence = traceId.getTransactionSequence();
            if (this.agentStartTime == traceId.getAgentStartTime()) {
                return this.encode(traceId.getTransactionSequence()); //如果agentTime也是一样的，可以再一次减少发送量
            }
            //带上时间，因为时间不一样
            return this.encode(traceId.getAgentStartTime(), transactionSequence);
        }
        //带上agentId，因为不在jvm内部
        return this.encode(traceId.getAgentId(), traceId.getAgentStartTime(), traceId.getTransactionSequence());
    }

}
