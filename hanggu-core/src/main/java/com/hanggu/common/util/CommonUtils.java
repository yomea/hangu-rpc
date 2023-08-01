package com.hanggu.common.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:16
 */
public final class CommonUtils {

    private CommonUtils() {
        throw new RuntimeException("不允许实例化！");
    }

    public static final String createServiceKey(String groupName, String interfaceName, String version) {

        return Arrays.asList(groupName, interfaceName, version)
            .stream().filter(e -> Objects.nonNull(e) && !e.trim().isEmpty()).collect(Collectors.joining("/"));
    }

}
