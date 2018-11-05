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

package com.navercorp.pinpoint.profiler.context.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.sampler.Sampler;
import com.navercorp.pinpoint.profiler.sampler.SamplerFactory;

/**
 * 采用提供器
 * @author Woonduk Kang(emeroad)
 */
public class SamplerProvider implements Provider<Sampler> {

    private final ProfilerConfig profilerConfig;

    @Inject
    public SamplerProvider(ProfilerConfig profilerConfig) {
        this.profilerConfig = profilerConfig;
    }

    @Override
    public Sampler get() {
        //是否采样
        boolean samplingEnable = profilerConfig.isSamplingEnable();
        //采样速率
        int samplingRate = profilerConfig.getSamplingRate();
        SamplerFactory samplerFactory = new SamplerFactory();
        //获得采样策略
        return samplerFactory.createSampler(samplingEnable, samplingRate);
    }
}
