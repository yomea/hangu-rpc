package com.hangu.consumer.http;

import com.google.common.collect.Maps;
import com.hangu.common.entity.MethodInfo;
import com.hangu.common.entity.ParameterInfo;
import com.hangu.common.entity.RequestHandlerInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.enums.MethodCallTypeEnum;
import com.hangu.common.exception.RpcInvokerException;
import com.hangu.common.properties.HanguProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.consumer.reference.ReferenceBean;
import com.hangu.consumer.reference.ServiceReference;
import com.hangu.entity.ServerMethodInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * @author wuzhenhong
 * @date 2024/2/8 11:44
 */
public class HttpGenericProxyFactory {

    private static final Map<String, HttpGenericService> HTTP_PROXY = new ConcurrentHashMap<>(8192);
    private static final Map<String, Object> LOCK = new ConcurrentHashMap<>(8192);
    private static final Object OBJECT = new Object();

    public static HttpGenericService httpProxy(javax.servlet.http.HttpServletRequest request,
        RegistryService registryService, HanguProperties hanguProperties) throws Exception {

        ServerMethodInfo serverMethodInfo = HttpGenericProxyFactory.parsePathVariables(request);
        String cacheKey = HttpGenericProxyFactory.createCacheKey(serverMethodInfo);
        HttpGenericService httpGenericService = HTTP_PROXY.get(cacheKey);
        if (Objects.nonNull(httpGenericService)) {
            return httpGenericService;
        }

        // 自旋
        while (LOCK.putIfAbsent(cacheKey, OBJECT) != null) {
            Thread.yield();
        }

        try {
            RequestHandlerInfo requestHandlerInfo = HttpGenericProxyFactory.buildRequestHandlerInfo(request, serverMethodInfo);
            ReferenceBean<HttpGenericService> referenceBean = new ReferenceBean<>(requestHandlerInfo,
                HttpGenericService.class, registryService, hanguProperties);
            HttpGenericService httpService = ServiceReference.reference(referenceBean);
            HTTP_PROXY.put(cacheKey, httpService);
            return httpService;
        } finally {
            LOCK.remove(cacheKey);
        }
    }

    private static String createCacheKey(ServerMethodInfo serverMethodInfo) {

        String argsSign = Optional.ofNullable(serverMethodInfo.getArgsNameList()).orElse(Collections.emptyList())
            .stream().collect(Collectors.joining(","));

        return String.format("%s_%s_%s_%s_%s",
            serverMethodInfo.getGroupName(),
            serverMethodInfo.getVersion(),
            serverMethodInfo.getInterfaceName(),
            serverMethodInfo.getMethodName(),
            argsSign
        );
    }

    private static ServerMethodInfo parsePathVariables(HttpServletRequest request) {

        // /groupName/version/interfaceName/methodName/generic/api
        String URI = request.getRequestURI();
        String[] pathVariableArr = URI.split("/");
        List<String> ablePatchVariables = Arrays.stream(pathVariableArr).filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        if (ablePatchVariables.size() < 4) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "url路径参数不正确，请使用形如 ”/groupName/version/interfaceName/methodName/generic/api“ 的url请求");
        }

        String methodName = ablePatchVariables.get(ablePatchVariables.size() - 3);
        String interfaceName = ablePatchVariables.get(ablePatchVariables.size() - 4);
        String groupName = "";
        String version = "";
        if (ablePatchVariables.size() > 4) {
            version = ablePatchVariables.get(ablePatchVariables.size() - 5);
        }
        if (ablePatchVariables.size() > 5) {
            groupName = ablePatchVariables.get(ablePatchVariables.size() - 6);
        }

        return ServerMethodInfo.builder()
            .methodName(methodName)
            .interfaceName(interfaceName)
            .version(version)
            .groupName(groupName)
            .build();
    }

    private static RequestHandlerInfo buildRequestHandlerInfo(HttpServletRequest request,
        ServerMethodInfo serverMethodInfo) throws Exception {

        RequestHandlerInfo requestHandlerInfo = new RequestHandlerInfo();

        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setGroupName(serverMethodInfo.getGroupName());
        serverInfo.setInterfaceName(serverMethodInfo.getInterfaceName());
        serverInfo.setVersion(serverMethodInfo.getVersion());
        MethodInfo providedMethodInfo = new MethodInfo();
        String argsSign = Optional.ofNullable(serverMethodInfo.getArgsNameList()).orElse(Collections.emptyList())
            .stream().collect(Collectors.joining(","));
        String methodSign = serverMethodInfo.getMethodName() + "(" + argsSign + ")";
        providedMethodInfo.setSign(methodSign);
        providedMethodInfo.setHttp(true);
        providedMethodInfo.setName(serverMethodInfo.getMethodName());
        providedMethodInfo.setTimeout(5);
        providedMethodInfo.setCallType(MethodCallTypeEnum.SYNC.getType());
        ParameterInfo parameterInfo = new ParameterInfo();
        parameterInfo.setIndex(0);
        parameterInfo.setType(com.hangu.common.entity.HttpServletRequest.class);

        com.hangu.common.entity.HttpServletRequest apiRequest = HttpGenericProxyFactory.buildRequest(request);
        parameterInfo.setValue(apiRequest);

        List<ParameterInfo> factParameterInfoList = Collections.singletonList(parameterInfo);
        providedMethodInfo.setFactParameterInfoList(factParameterInfoList);

        requestHandlerInfo.setProvidedMethodInfo(providedMethodInfo);
        requestHandlerInfo.setServerInfo(serverInfo);

        return requestHandlerInfo;
    }

    private static com.hangu.common.entity.HttpServletRequest buildRequest(HttpServletRequest request)
        throws Exception {
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

        return apiReqest;
    }
}
