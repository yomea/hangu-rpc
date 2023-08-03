package com.hanggu.consumer.callback;

import com.hanggu.common.entity.RpcResult;

/**
 * @author wuzhenhong
 * @date 2023/8/3 16:13
 */
@FunctionalInterface
public interface RpcResponseCallback {

    void callback(RpcResult rpcResult);
}
