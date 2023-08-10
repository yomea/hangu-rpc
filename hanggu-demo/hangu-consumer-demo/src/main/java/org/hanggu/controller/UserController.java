package org.hanggu.controller;

import java.util.HashMap;
import java.util.Map;
import org.hanggu.consumer.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/name")
    public Map<String, Object> getName() {
        String name = userService.getName();
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        return map;
    }

}
