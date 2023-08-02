package com.hanggu.consumer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/2 8:42
 */
@ConfigurationProperties(prefix = "hangu.consumer")
@Data
public class ConsumerProperties {

}
