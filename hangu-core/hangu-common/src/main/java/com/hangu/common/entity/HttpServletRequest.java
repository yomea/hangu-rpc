package com.hangu.common.entity;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2024/2/7 14:37
 */
@Data
public class HttpServletRequest implements Serializable {

    private String method;
    private Map<String, String> heads;
    private Map<String, String[]> getParam;
    private byte[] bodyData;
    private String URI;
}
