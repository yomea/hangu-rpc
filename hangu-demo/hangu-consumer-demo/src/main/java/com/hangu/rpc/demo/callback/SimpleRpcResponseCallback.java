package com.hangu.rpc.demo.callback;

import cn.hutool.json.JSONUtil;
import com.hangu.rpc.common.callback.RpcResponseCallback;
import com.hangu.rpc.common.entity.RpcResult;

/**
 * @author wuzhenhong
 * @date 2023/8/10 14:37
 */
public class SimpleRpcResponseCallback implements RpcResponseCallback {

    @Override
    public void callback(RpcResult rpcResult) {

        System.out.println("SimpleRpcResponseCallback --------" + JSONUtil.toJsonStr(rpcResult));
    }
}
