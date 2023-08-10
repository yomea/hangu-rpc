package org.hanggu.provider;

import com.hanggu.consumer.callback.RpcResponseCallback;
import com.hanggu.provider.annotation.HangguService;
import java.util.List;
import org.hanggu.consumer.UserService;
import org.hanggu.entity.Address;
import org.hanggu.entity.UserInfo;

/**
 * @author wuzhenhong
 * @date 2023/8/4 15:21
 */
@HangguService
public class UserServiceImpl implements UserService {

    @Override
    public UserInfo getUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("小风");
        userInfo.setAge(27);
        Address address = new Address();
        address.setProvince("江西省");
        address.setCity("赣州市");
        address.setArea("于都县");
        userInfo.setAddress(address);
        return userInfo;
    }

    @Override
    public String getUserInfo(String name) {
        return name;
    }

    @Override
    public Address getUserAddrss(String city, String area) {
        Address address = new Address();
        address.setProvince("江西省");
        address.setCity(city);
        address.setArea(area);
        return address;
    }

    @Override
    public UserInfo getUserInfo(String name, int age) {
        UserInfo userInfo = new UserInfo();
        userInfo.setName(name);
        userInfo.setAge(age);
        return userInfo;
    }

    @Override
    public UserInfo getUserInfo(UserInfo userInfo) {
        return userInfo;
    }

    @Override
    public List<UserInfo> getUserInfos(List<UserInfo> userInfos) {
        return userInfos;
    }
}
