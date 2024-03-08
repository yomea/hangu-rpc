package com.hangu.rpc.common.callback;

import com.hangu.rpc.common.entity.RpcResult;

/**
 * @author wuzhenhong
 * @date 2023/8/3 16:13
 */
@FunctionalInterface
public interface RpcResponseCallback {

    void callback(RpcResult rpcResult);
}
