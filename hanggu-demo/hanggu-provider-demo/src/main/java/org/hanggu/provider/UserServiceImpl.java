package org.hanggu.provider;

import com.hanggu.consumer.callback.RpcResponseCallback;
import com.hanggu.provider.annotation.HangguService;
import org.hanggu.consumer.UserService;

/**
 * @author wuzhenhong
 * @date 2023/8/4 15:21
 */
@HangguService
public class UserServiceImpl implements UserService {

    @Override
    public String getName(RpcResponseCallback callback) {
        return "小风";
    }
}
