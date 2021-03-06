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

package com.navercorp.pinpoint.profiler.context;

/**
 * @author Woonduk Kang(emeroad)
 */
public interface CallStack {

    /**
     * 获得当前的的序号
     * @return
     */
    int getIndex();

    /**
     * 将一个span事件推入调用栈中
     * @param spanEvent
     * @return
     */
    int push(SpanEvent spanEvent);

    /**
     * 栈中弹出一个事件
     * @return
     */
    SpanEvent pop();

    SpanEvent peek();

    boolean empty();

    SpanEvent[] copyStackFrame();

    /**
     * 获得最大栈深度
     * @return
     */
    int getMaxDepth();
}
