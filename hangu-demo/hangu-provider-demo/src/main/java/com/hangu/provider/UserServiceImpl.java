package com.hangu.provider;

import com.hangu.common.entity.HttpServletRequest;
import com.hangu.common.entity.HttpServletResponse;
import com.hangu.consumer.UserService;
import com.hangu.entity.Address;
import com.hangu.entity.UserInfo;
import com.hangu.provider.annotation.HanguService;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author wuzhenhong
 * @date 2023/8/4 15:21
 */
@HanguService
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

    @Override
    public UserInfo xxx(String name, int age) {
        UserInfo userInfo = new UserInfo();
        userInfo.setName(name);
        userInfo.setAge(age);
        return userInfo;
    }

    @Override
    public UserInfo yyy(UserInfo userInfo) {
        return userInfo;
    }

    @Override
    public UserInfo zzz(UserInfo userInfo, HttpServletRequest request, HttpServletResponse response) {

        response.addHeader("Content-Disposition", "attachment;filename=xx.txt");
        response.addHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "application/vnd.ms-excel");
        response.setBodyData("hello world!".getBytes(StandardCharsets.UTF_8));
        return userInfo;
    }
}
