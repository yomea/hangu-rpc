package com.hangu.rpc.demo.entity;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/10 14:38
 */
@Data
public class UserInfo implements Serializable {

    private String name;
    private List<String> aliasNames;

    private int age;

    private Address address;
}
