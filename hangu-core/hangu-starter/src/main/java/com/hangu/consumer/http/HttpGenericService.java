package com.hangu.consumer.http;

import com.hangu.common.entity.HttpServletRequest;

/**
 * http泛化调用
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
public interface HttpGenericService {

   String http(HttpServletRequest request);
}
