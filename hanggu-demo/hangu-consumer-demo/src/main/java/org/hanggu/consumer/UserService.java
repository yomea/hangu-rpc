package org.hanggu.consumer;

import com.hanggu.consumer.annotation.HangguReference;
import com.hanggu.consumer.annotation.HanguMethod;
import com.hanggu.consumer.callback.RpcResponseCallback;
import java.util.List;
import org.hanggu.callback.SimpleRpcResponseCallback;
import org.hanggu.entity.Address;
import org.hanggu.entity.UserInfo;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
@HangguReference
public interface UserService {

    @HanguMethod(timeout = 20, callback = SimpleRpcResponseCallback.class)
    UserInfo getUserInfo(RpcResponseCallback callback);
    String getUserInfo(String name);

    Address getUserAddrss(String city, String area);
    UserInfo getUserInfo(String name, int age);

    UserInfo getUserInfo(UserInfo userInfo);

    List<UserInfo> getUserInfos(List<UserInfo> userInfos);
}
