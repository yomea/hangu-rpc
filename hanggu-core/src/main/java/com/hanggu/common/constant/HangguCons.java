package com.hanggu.common.constant;

import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:53
 */
public final class HangguCons {

    /**
     * 魔数
     */
    public static final short MAGIC = (short) 0xabcd;

    /**
     * 序列化类型掩码
     */
    public static final short SERIALIZATION_MARK = (short) 0x000F;

    public static final int DEF_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    public static final int CPUS = Runtime.getRuntime().availableProcessors();
}
