package com.navercorp.pinpoint.profiler.instrument;

/**
 * 字节码导出服务
 * @author Woonduk Kang(emeroad)
 */
public interface BytecodeDumpService {

    /**
     * 导出服务
     * @param dumpMessage
     * @param jvmClassName
     * @param bytes
     * @param classLoader
     */
    void dumpBytecode(String dumpMessage, String jvmClassName, byte[] bytes, ClassLoader classLoader);
}
