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

package com.navercorp.pinpoint.profiler.metadata;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.thrift.dto.TApiMetaData;

/**
 * 默认的api的调用元数据服务
 * @author Woonduk Kang(emeroad)
 */
public class DefaultApiMetaDataService implements ApiMetaDataService {

    private final SimpleCache<String> apiCache = new SimpleCache<String>();

    //agent的id
    private final String agentId;
    //agent开始时间
    private final long agentStartTime;
    //增强型的数据发送器
    private final EnhancedDataSender enhancedDataSender;

    public DefaultApiMetaDataService(String agentId, long agentStartTime, EnhancedDataSender enhancedDataSender) {
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }
        if (enhancedDataSender == null) {
            throw new NullPointerException("enhancedDataSender must not be null");
        }
        this.agentId = agentId;
        this.agentStartTime = agentStartTime;
        this.enhancedDataSender = enhancedDataSender;
    }

    /**
     * 缓存api信息
     * @param methodDescriptor
     * @return
     */
    @Override
    public int cacheApi(final MethodDescriptor methodDescriptor) {
        //获得键，方法名
        final String fullName = methodDescriptor.getFullName();
        //获得存储结果，无则新建
        final Result result = this.apiCache.put(fullName);

        //设置apiID
        methodDescriptor.setApiId(result.getId());

        //新生成的
        if (result.isNewValue()) {
            final TApiMetaData apiMetadata = new TApiMetaData();
            apiMetadata.setAgentId(agentId);
            apiMetadata.setAgentStartTime(agentStartTime);

            apiMetadata.setApiId(result.getId());
            apiMetadata.setApiInfo(methodDescriptor.getApiDescriptor());
            apiMetadata.setLine(methodDescriptor.getLineNumber());
            apiMetadata.setType(methodDescriptor.getType());

            //使用TCP使用thrif进行调用发送
            this.enhancedDataSender.request(apiMetadata);
        }

        return result.getId();
    }
}
