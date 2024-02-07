package com.hangu.controller;

import com.google.common.collect.Maps;
import com.hangu.common.entity.RequestHandlerInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.properties.HanguProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.util.CommonUtils;
import com.hangu.consumer.HttpService;
import com.hangu.consumer.UserService;
import com.hangu.consumer.properties.ConsumerProperties;
import com.hangu.consumer.reference.ReferenceBean;
import com.hangu.consumer.reference.ServiceReference;
import com.hangu.entity.UserInfo;
import com.hangu.provider.binder.WebDataBinder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RegistryService registryService;

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

    @GetMapping("/h")
    public Object h(HttpServletRequest request) throws IOException {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setGroupName("");
        serverInfo.setInterfaceName("com.hangu.consumer.UserService");
        serverInfo.setVersion("");
        HanguProperties hanguProperties = new HanguProperties();
        hanguProperties.setConsumer(new ConsumerProperties());
        hanguProperties.setCoreNum(4);
        hanguProperties.setMaxNum(20);
        RequestHandlerInfo requestHandlerInfo = new RequestHandlerInfo();
        requestHandlerInfo.setHttp(true);
        requestHandlerInfo.setMethodName("yyy");
        requestHandlerInfo.setServerInfo(serverInfo);
        ReferenceBean<HttpService> referenceBean = new ReferenceBean<>(requestHandlerInfo,
            HttpService.class, registryService, hanguProperties);
        HttpService httpService = ServiceReference.reference(referenceBean);

        com.hangu.common.entity.HttpServletRequest apiReqest = new com.hangu.common.entity.HttpServletRequest();
        apiReqest.setMethod(request.getMethod());
        apiReqest.setURI(request.getRequestURI());

        apiReqest.setGetParam(request.getParameterMap());


        InputStream inputStream = request.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] tmp = new byte[2048];
        int i = inputStream.read(tmp);
        while (i > 0) {
            baos.write(tmp, 0, i);
            i = inputStream.read(tmp);
        }
        apiReqest.setBodyData(baos.toByteArray());


        Enumeration<String> enumeration = request.getHeaderNames();
        Map<String, String> header = Maps.newHashMap();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            header.put(key.toLowerCase(), request.getHeader(key));
        }

        apiReqest.setHeads(header);

        Object t = httpService.http(apiReqest);
        return t;
    }


}
