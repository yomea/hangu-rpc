package org.hanggu.callback;

import cn.hutool.json.JSONUtil;
import com.hanggu.common.entity.RpcResult;
import com.hanggu.consumer.callback.RpcResponseCallback;

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
