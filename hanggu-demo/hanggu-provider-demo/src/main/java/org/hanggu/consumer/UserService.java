package org.hanggu.consumer;

import java.util.List;
import org.hanggu.entity.Address;
import org.hanggu.entity.UserInfo;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
public interface UserService {

    UserInfo getUserInfo();

    String getUserInfo(String name);

    Address getUserAddrss(String city, String area);

    UserInfo getUserInfo(String name, int age);

    UserInfo getUserInfo(UserInfo userInfo);

    List<UserInfo> getUserInfos(List<UserInfo> userInfos);
}
