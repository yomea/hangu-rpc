package com.hangu.common.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wuzhenhong
 * @date 2024/2/8 13:54
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerMethodInfo {

    private String groupName;

    private String interfaceName;

    private String version;

    private String methodName;

    private List<String> argsNameList;
}
