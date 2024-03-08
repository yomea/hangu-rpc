package com.hangu.rpc.demo.consumer;

import com.hangu.rpc.demo.callback.SimpleRpcResponseCallback;
import com.hangu.rpc.common.callback.RpcResponseCallback;
import com.hangu.rpc.common.entity.HttpServletRequest;
import com.hangu.rpc.common.entity.HttpServletResponse;
import com.hangu.rpc.consumer.annotation.HanguMethod;
import com.hangu.rpc.starter.consumer.annotation.HanguReference;
import com.hangu.rpc.demo.entity.Address;
import com.hangu.rpc.demo.entity.UserInfo;
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

    UserInfo xxx(String name, int age);

    UserInfo yyy(UserInfo userInfo);

    UserInfo zzz(UserInfo userInfo, HttpServletRequest request, HttpServletResponse response);
}
