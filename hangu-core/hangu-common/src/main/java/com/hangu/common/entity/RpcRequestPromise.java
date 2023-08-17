package com.hangu.common.entity;

import com.hangu.common.callback.RpcResponseCallback;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import java.util.List;

/**
 * @author wuzhenhong
 * @date 2023/8/3 17:36
 */
public class RpcRequestPromise<V> extends DefaultPromise<V> {

    private List<RpcResponseCallback> callbacks;

    public RpcRequestPromise(EventExecutor executor) {
        super(executor);
    }

    public RpcRequestPromise(List<RpcResponseCallback> callbacks, EventExecutor executor) {
        super(executor);
        this.callbacks = callbacks;
    }

    public List<RpcResponseCallback> getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(List<RpcResponseCallback> callbacks) {
        this.callbacks = callbacks;
    }
}
