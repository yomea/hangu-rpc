package com.hangu.consumer;

import com.hangu.entity.Address;
import com.hangu.entity.UserInfo;
import java.util.List;

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

    UserInfo xxx(String name, int age);
    UserInfo yyy(UserInfo userInfo);
}
