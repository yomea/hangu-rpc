package com.hangu.consumer;

import com.hangu.callback.SimpleRpcResponseCallback;
import com.hangu.common.callback.RpcResponseCallback;
import com.hangu.common.entity.HttpServletRequest;
import com.hangu.consumer.annotation.HanguMethod;
import com.hangu.consumer.annotation.HanguReference;
import com.hangu.entity.Address;
import com.hangu.entity.UserInfo;
import java.util.List;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:47
 */
public interface HttpService {

   Object http(HttpServletRequest request);
}
