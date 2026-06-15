package com.vnpay.sit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sit.bootstrap")
public class SitBootstrapProperties {

    private String adminEmail = "";
    private String adminPassword = "";
    private String adminFullName = "System Admin";
}
