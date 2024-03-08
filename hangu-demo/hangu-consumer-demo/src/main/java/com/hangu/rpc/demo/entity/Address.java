package com.hangu.rpc.demo.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/10 14:38
 */
@Data
public class Address implements Serializable {

    private String province;

    private String city;

    private String area;

}
