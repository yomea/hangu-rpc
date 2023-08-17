package com.hangu.consumer;

import com.hangu.callback.SimpleRpcResponseCallback;
import com.hangu.common.callback.RpcResponseCallback;
import com.hangu.consumer.annotation.HanguMethod;
import com.hangu.consumer.annotation.HanguReference;
import com.hangu.entity.Address;
import com.hangu.entity.UserInfo;
import java.util.List;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
@HanguReference
public interface UserService {

    @HanguMethod(timeout = 20, callback = SimpleRpcResponseCallback.class)
    UserInfo getUserInfo(RpcResponseCallback callback);

    String getUserInfo(String name);

    Address getUserAddrss(String city, String area);

    UserInfo getUserInfo(String name, int age);

    UserInfo getUserInfo(UserInfo userInfo);

    List<UserInfo> getUserInfos(List<UserInfo> userInfos);
}
