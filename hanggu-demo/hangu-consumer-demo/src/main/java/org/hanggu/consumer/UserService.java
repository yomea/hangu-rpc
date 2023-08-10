package org.hanggu.consumer;

import com.hanggu.consumer.annotation.HangguReference;
import com.hanggu.consumer.callback.RpcResponseCallback;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
@HangguReference
public interface UserService {

    String getName();

}
