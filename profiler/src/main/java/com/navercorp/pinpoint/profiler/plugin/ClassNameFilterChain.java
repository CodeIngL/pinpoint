/*
 * Copyright 2016 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤器链类名过滤，组合了pinpoint本身和插件写点。
 * @author Woonduk Kang(emeroad)
 */
public class ClassNameFilterChain implements ClassNameFilter {

    private final List<ClassNameFilter> filterChain;

    public ClassNameFilterChain(List<ClassNameFilter> filterChain) {
        if (filterChain == null) {
            throw new NullPointerException("filterChain must not be null");
        }
        this.filterChain = new ArrayList<ClassNameFilter>(filterChain);
    }


    @Override
    public boolean accept(String className) {
        for (ClassNameFilter classNameFilter : this.filterChain) {
            if (!classNameFilter.accept(className)) {
                return REJECT;
            }
        }
        return ACCEPT;
    }
}
