package org.hangu.consumer;

import com.hangu.consumer.annotation.hanguReference;
import com.hangu.consumer.annotation.HanguMethod;
import com.hangu.common.callback.RpcResponseCallback;
import java.util.List;
import org.hangu.callback.SimpleRpcResponseCallback;
import org.hangu.entity.Address;
import org.hangu.entity.UserInfo;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
@hanguReference
public interface UserService {

    @HanguMethod(timeout = 20, callback = SimpleRpcResponseCallback.class)
    UserInfo getUserInfo(RpcResponseCallback callback);

    String getUserInfo(String name);

    Address getUserAddrss(String city, String area);

    UserInfo getUserInfo(String name, int age);

    UserInfo getUserInfo(UserInfo userInfo);

    List<UserInfo> getUserInfos(List<UserInfo> userInfos);
}
