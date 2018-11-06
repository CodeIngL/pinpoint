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

package com.navercorp.pinpoint.common.service;

import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeInfo;
import com.navercorp.pinpoint.common.trace.ServiceTypeRegistry;
import com.navercorp.pinpoint.common.util.logger.CommonLogger;
import com.navercorp.pinpoint.common.util.logger.CommonLoggerFactory;
import com.navercorp.pinpoint.common.util.StaticFieldLookUp;
import com.navercorp.pinpoint.common.util.logger.StdoutCommonLoggerFactory;

import java.util.List;


/**
 * 默认的插件类型注册表的服务
 * @author emeroad
 */
public class DefaultServiceTypeRegistryService implements ServiceTypeRegistryService {
    private final CommonLogger logger;

    //追踪元数据服务
    private final TraceMetadataLoaderService typeLoaderService;
    //ServiceTypeRegistry注册表
    private final ServiceTypeRegistry registry;

    public DefaultServiceTypeRegistryService() {
        this(new DefaultTraceMetadataLoaderService(), StdoutCommonLoggerFactory.INSTANCE);
    }


    /**
     * 构建ServiceType注册服务
     * @param typeLoaderService
     * @param commonLoggerFactory
     */
    public DefaultServiceTypeRegistryService(TraceMetadataLoaderService typeLoaderService, CommonLoggerFactory commonLoggerFactory) {
        if (typeLoaderService == null) {
            throw new NullPointerException("typeLoaderService must not be null");
        }
        if (commonLoggerFactory == null) {
            throw new NullPointerException("commonLoggerFactory must not be null");
        }
        this.logger = commonLoggerFactory.getLogger(DefaultServiceTypeRegistryService.class.getName());
        this.typeLoaderService = typeLoaderService;
        //初始化注册表
        this.registry = buildServiceTypeRegistry();
    }

    /**
     * 构建注册表
     * @return
     */
    private ServiceTypeRegistry buildServiceTypeRegistry() {
        ServiceTypeRegistry.Builder builder = new ServiceTypeRegistry.Builder();

        //默认的ServiceType
        StaticFieldLookUp<ServiceType> staticFieldLookUp = new StaticFieldLookUp<ServiceType>(ServiceType.class, ServiceType.class);
        List<ServiceType> lookup = staticFieldLookUp.lookup();
        for (ServiceType serviceType: lookup) {
            if (logger.isInfoEnabled()) {
                logger.info("add Default ServiceType:" + serviceType);
            }
            builder.addServiceType(serviceType);
        }

        //插件的ServiceType
        final List<ServiceTypeInfo> types = loadType();
        for (ServiceTypeInfo type : types) {
            if (logger.isInfoEnabled()) {
                logger.info("add Plugin ServiceType:" + type.getServiceType());
            }
            builder.addServiceType(type.getServiceType());
        }


        return builder.build();
    }


    private List<ServiceTypeInfo> loadType() {
        return typeLoaderService.getServiceTypeInfos();
    }

    @Override
    public ServiceType findServiceType(short serviceType) {
        return registry.findServiceType(serviceType);
    }

    public ServiceType findServiceTypeByName(String typeName) {
        return registry.findServiceTypeByName(typeName);
    }

    @Override
    public List<ServiceType> findDesc(String desc) {
        return registry.findDesc(desc);
    }

}
