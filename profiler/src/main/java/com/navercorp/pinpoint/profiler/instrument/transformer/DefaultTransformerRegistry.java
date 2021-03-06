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

package com.navercorp.pinpoint.profiler.instrument.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.navercorp.pinpoint.bootstrap.instrument.matcher.ClassNameMatcher;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matcher;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.MultiClassNameMatcher;
import com.navercorp.pinpoint.profiler.instrument.classreading.InternalClassMetadata;
import com.navercorp.pinpoint.profiler.util.JavaAssistUtils;

/**
 *
 * 最普通的注册器，痛处的类处理将会从这里进行获得相应的匹配器，
 * 目前为止这是基于匹配器的。
 * @author emeroad
 * @author netspider
 * @author hyungil.jeong
 * @author Minwoo Jung
 * @author jaehong.kim
 */
public class DefaultTransformerRegistry implements TransformerRegistry {

    // No concurrent issue because only one thread put entries to the map and get operations are started AFTER the map is completely build.
    // Set the map size big intentionally to keep hash collision low.
    // 没有并发问题，因为只有一个线程将entry放入map,并且在完全构建map后开始获取操作。
    // 有意设置map大小以保持哈希冲突低。
    private final Map<String, ClassFileTransformer> registry = new HashMap<String, ClassFileTransformer>(512);

    @Override
    public ClassFileTransformer findTransformer(final ClassLoader classLoader, final String classInternalName, final byte[] classFileBuffer) {
        return findTransformer(classLoader, classInternalName, classFileBuffer, null);
    }

    @Override
    public ClassFileTransformer findTransformer(ClassLoader classLoader, String classInternalName, byte[] classFileBuffer, InternalClassMetadata classMetadata) {
        return registry.get(classInternalName);
    }

    public void addTransformer(Matcher matcher, ClassFileTransformer transformer) {
        // TODO extract matcher process
        if (matcher instanceof ClassNameMatcher) {
            final ClassNameMatcher classNameMatcher = (ClassNameMatcher) matcher;
            String className = classNameMatcher.getClassName();
            addModifier0(transformer, className);
        } else if (matcher instanceof MultiClassNameMatcher) {
            final MultiClassNameMatcher classNameMatcher = (MultiClassNameMatcher) matcher;
            List<String> classNameList = classNameMatcher.getClassNames();
            for (String className : classNameList) {
                addModifier0(transformer, className);
            }
        } else {
            throw new IllegalArgumentException("unsupported matcher :" + matcher);
        }
    }

    private void addModifier0(ClassFileTransformer transformer, String className) {
        final String classInternalName = JavaAssistUtils.javaNameToJvmName(className);
        ClassFileTransformer old = registry.put(classInternalName, transformer);

        if (old != null) {
            throw new IllegalStateException("Transformer already exist. className:" + classInternalName + " new:" + transformer.getClass() + " old:" + old.getClass());
        }
    }
}