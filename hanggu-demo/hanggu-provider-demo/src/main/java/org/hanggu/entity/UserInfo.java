package org.hanggu.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/10 14:38
 */
@Data
public class UserInfo {

    private String name;

    private int age;

    private Address address;
}
