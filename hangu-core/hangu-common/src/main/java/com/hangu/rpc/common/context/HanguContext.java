package com.hangu.rpc.common.context;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wuzhenhong
 * @date 2023/8/9 17:40
 */
public class HanguContext {

    public static final String DYNAMIC_TIME_OUT = "DYNAMIC_TIME_OUT";

    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return new HashMap<>();
        }
    };

    public static <T> T getValue(String key) {
        return (T) CONTEXT.get().get(key);
    }

    public static <T> T putValue(String key, T value) {
        return (T) CONTEXT.get().put(key, value);
    }

    public static void remove() {
        CONTEXT.remove();
    }
}
