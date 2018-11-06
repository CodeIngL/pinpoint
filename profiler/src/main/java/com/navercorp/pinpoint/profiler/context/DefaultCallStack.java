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

import com.navercorp.pinpoint.common.annotations.VisibleForTesting;
import com.navercorp.pinpoint.profiler.context.id.TraceRoot;

import java.util.Arrays;

/**
 *
 * 默认的调用栈实现
 *
 * @author netspider
 * @author Woonduk Kang(emeroad)
 * @author jaehong.kim
 */
public class DefaultCallStack implements CallStack {

    //栈大小为8
    protected static final int STACK_SIZE = 8;
    //默认的序号
    protected static final int DEFAULT_INDEX = 0;

    protected SpanEvent[] stack = new SpanEvent[STACK_SIZE];

    protected final TraceRoot traceRoot;
    protected final int maxDepth;
    protected int index = DEFAULT_INDEX;
    protected int overflowIndex = 0;
    protected short sequence;

    public DefaultCallStack(TraceRoot traceRoot) {
        this(traceRoot, -1);
    }

    public DefaultCallStack(TraceRoot traceRoot, int maxDepth) {
        this.traceRoot = traceRoot;
        this.maxDepth = maxDepth;
    }


    @Override
    public int getIndex() {
        if (isOverflow()) {
            return index + overflowIndex;
        }

        return index;
    }

    /**
     * 向调用栈中华追加一个spanevent事件
     * @param spanEvent
     * @return
     */
    @Override
    public int push(final SpanEvent spanEvent) {
        if (isOverflow()) { //是否溢出
            overflowIndex++;
            return index + overflowIndex;
        }

        checkExtend(index + 1);
        spanEvent.setSequence(sequence++);
        stack[index++] = spanEvent;
        markDepth(spanEvent, index);
        return index;
    }


    /**
     * 标记spanEvent在栈中的深度
     * @param spanEvent
     * @param index
     */
    protected void markDepth(SpanEvent spanEvent, int index) {
        spanEvent.setDepth(index);
    }

    /**
     * 容量校验，超过时进行翻倍
     * @param size 追加一个后的大小
     */
    protected void checkExtend(final int size) {
        final SpanEvent[] originalStack = this.stack;
        if (size >= originalStack.length) {
            final int copyStackSize = originalStack.length << 1;
            final SpanEvent[] copyStack = new SpanEvent[copyStackSize];
            System.arraycopy(originalStack, 0, copyStack, 0, originalStack.length);
            this.stack = copyStack;
        }
    }

    /**
     * 栈中弹出一个spanevent
     * @return
     */
    @Override
    public SpanEvent pop() {
        if (isOverflow() && overflowIndex > 0) {
            overflowIndex--;
            return newDummySpanEvent();
        }

        final SpanEvent spanEvent = peek();
        if (spanEvent != null) {
            stack[index - 1] = null;
            index--;
        }

        return spanEvent;
    }

    private SpanEvent newDummySpanEvent() {
        return new SpanEvent(traceRoot);
    }

    @Override
    public SpanEvent peek() {
        if (index == DEFAULT_INDEX) {
            return null;
        }

        //如果溢出了，返回一个假的spanevent事件
        if (isOverflow() && overflowIndex > 0) {
            return newDummySpanEvent();
        }

        return stack[index - 1];
    }

    @Override
    public boolean empty() {
        return index == DEFAULT_INDEX;
    }

    /**
     * 拷贝栈帧
     * @return
     */
    @Override
    public SpanEvent[] copyStackFrame() {
        // without synchronization arraycopy, last index is null reference
        final SpanEvent[] currentStack = this.stack;
        final SpanEvent[] copyStack = new SpanEvent[currentStack.length];
        System.arraycopy(currentStack, 0, copyStack, 0, currentStack.length);
        return copyStack;
    }

    @Override
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * 是否溢出
     * @return 是否溢出
     */
    @VisibleForTesting
    boolean isOverflow() {
        return maxDepth != -1 && maxDepth < index;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{stack=");
        builder.append(Arrays.toString(stack));
        builder.append(", index=");
        builder.append(index);
        builder.append("}");
        return builder.toString();
    }
}