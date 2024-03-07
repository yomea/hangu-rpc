package com.hangu.controller;

import com.hangu.common.entity.HttpServletResponse;
import com.hangu.common.properties.ExecutorProperties;
import com.hangu.common.properties.HanguProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.consumer.UserService;
import com.hangu.consumer.http.HttpGenericProxyFactory;
import com.hangu.consumer.http.HttpGenericService;
import com.hangu.entity.UserInfo;
import com.hangu.provider.binder.WebDataBinder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.DefaultConversionService;
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
@RequestMapping(value = "/")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private HanguProperties hanguProperties;

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
    public com.hangu.entity.Address c() {
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

    @PostMapping("/g")
    public UserInfo g(HttpServletRequest request) {
        UserInfo userInfo = new UserInfo();
        Map<String, String[]> map = request.getParameterMap();
        MutablePropertyValues mpvs = new MutablePropertyValues();
        map.forEach((k, v) -> {
            Arrays.stream(v).forEach(value -> {
                mpvs.add(k, value);
            });
        });

        WebDataBinder webDataBinder = new WebDataBinder(userInfo, new DefaultConversionService());
        webDataBinder.bind(mpvs);
        return userInfo;
    }

    @GetMapping("/{interfaceName}/{methodName}/generic/api")
    public void h(HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws Exception {
        com.hangu.common.entity.HttpServletRequest apiRequest = HttpGenericProxyFactory.buildRequest(request);
        ExecutorProperties executorProperties = new ExecutorProperties();
        executorProperties.setMaxNum(hanguProperties.getMaxNum());
        executorProperties.setCoreNum(hanguProperties.getCoreNum());
        HttpGenericService httpProxy = HttpGenericProxyFactory.httpProxy(request.getRequestURI(), registryService,
            executorProperties);
        HttpServletResponse apiResponse = httpProxy.http(apiRequest);
        Optional.ofNullable(apiResponse.getHeads())
            .orElse(Collections.emptyMap()).forEach(response::addHeader);
        response.getOutputStream().write(apiResponse.getBodyData());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }


}
