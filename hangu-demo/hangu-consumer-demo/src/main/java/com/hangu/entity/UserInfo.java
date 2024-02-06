package com.hangu.entity;

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
    private List<String> names;

    private int age;

    private Address address;
}
