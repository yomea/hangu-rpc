package com.hangu.rpc.consumer.http;

import com.hangu.rpc.common.entity.HttpServletRequest;
import com.hangu.rpc.common.entity.HttpServletResponse;

/**
 * http泛化调用
 *
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
public interface HttpGenericService {

    HttpServletResponse http(HttpServletRequest request);
}
