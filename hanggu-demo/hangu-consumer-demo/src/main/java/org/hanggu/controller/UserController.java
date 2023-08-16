package org.hanggu.controller;

import java.util.List;
import org.hanggu.consumer.UserService;
import org.hanggu.entity.Address;
import org.hanggu.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wuzhenhong
 * @date 2023/8/10 9:28
 */
@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/a")
    public UserInfo a() {
        UserInfo userInfo = userService.getUserInfo((RpcResult) -> {
            System.out.println("8888888888888888888888888888888888888888888888888");
        });
        return userInfo;
    }

    @GetMapping("/b")
    public String b(@RequestParam("name") String name) {
        return userService.getUserInfo(name);
    }

    @GetMapping("/c")
    public Address c() {
        return userService.getUserAddrss("赣州市", "于都县");
    }

    @GetMapping("/d")
    public UserInfo d() {
        return userService.getUserInfo("小风", 18);
    }

    @PostMapping("/e")
    public UserInfo e(@RequestBody UserInfo userInfo) {
        return userService.getUserInfo(userInfo);
    }

    @PostMapping("/f")
    public List<UserInfo> f(@RequestBody List<UserInfo> userInfos) {
        return userService.getUserInfos(userInfos);
    }
}
