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

package com.navercorp.pinpoint.bootstrap.plugin.monitor;

import com.navercorp.pinpoint.common.trace.ServiceType;

/**
 * 数据源监控
 * @author Taejin Koo
 */
public interface DataSourceMonitor {

    /**
     * 获得服务类型
     * @return
     */
    ServiceType getServiceType();

    /**
     * 获得url
     * @return
     */
    String getUrl();

    /**
     * 获得活跃的连接数
     * @return
     */
    int getActiveConnectionSize();

    /**
     * 获得最大的连接数
     * @return
     */
    int getMaxConnectionSize();

    /**
     * 是否开启
     * @return
     */
    boolean isDisabled();


}
