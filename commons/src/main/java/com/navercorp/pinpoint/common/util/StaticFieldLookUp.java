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

package com.navercorp.pinpoint.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 静态字段寻找其
 * @author emeroad
 */
public class StaticFieldLookUp<T> {

    public interface Filter<T>  {
        boolean FILTERED = true;
        boolean INCLUDE = false;
        boolean filter(T serviceType);
    }

    public static class BypassFilter<T> implements Filter<T> {
        @Override
        public boolean filter(T type) {
            return INCLUDE;
        }
    }

    public static class ExcludeFilter<T> implements Filter<T> {
        private final T[] excludeTypeList;

        public ExcludeFilter(T[] excludeTypeList) {
            if (excludeTypeList == null) {
                throw new NullPointerException("excludeTypeList must not be null");
            }
             this.excludeTypeList = excludeTypeList;
        }

        @Override
        public boolean filter(T type) {
            for (T excludeType : excludeTypeList) {
                if (excludeType == type) {
                    return FILTERED;
                }
            }
            return Filter.INCLUDE;
        }
    }

    //目标类
    private final Class<?> targetClazz;
    //查找类
    private final Class<T> findClazz;

    /**
     * 静态字段寻找器
     * @param targetClazz 目标类
     * @param findClazz 查找类
     */
    public StaticFieldLookUp(Class<?> targetClazz, Class<T> findClazz) {
        if (targetClazz == null) {
            throw new NullPointerException("targetClazz must not be null");
        }
        if (findClazz == null) {
            throw new NullPointerException("findClazz must not be null");
        }
        this.targetClazz = targetClazz;
        this.findClazz = findClazz;
    }

    /**
     *
     * @param filter
     * @return
     */
    public List<T> lookup(Filter<T> filter) {
        if (filter == null) {
            throw new NullPointerException("filter must not be null");
        }
        final List<T> lookup = new ArrayList<T>();

        //获得目标类的字段
        Field[] declaredFields = targetClazz.getDeclaredFields();
        for (Field field : declaredFields) {
            //忽略非静态
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            //狐狸公开的
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            //忽略类型不是查找类型的
            if (!field.getType().equals(findClazz)) {
                continue;
            }
            // 需要 public static 字段类型是findclass类型
            final Object filedObject = getObject(field);

            //两者能转换，进行强转
            if (findClazz.isInstance(filedObject)) {
                T type = findClazz.cast(filedObject);
                //过滤这些类型
                if (filter.filter(type) == Filter.FILTERED) {
                    continue;
                }

                lookup.add(type);
            }
        }
        return lookup;
    }

    public List<T> lookup() {
        return lookup(new BypassFilter<T>());
    }

    private Object getObject(Field field) {
        try {
            return field.get(findClazz);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("service access fail. Caused by:" + ex.getMessage(), ex);
        }
    }



}
