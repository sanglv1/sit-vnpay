package com.vnpay.sit;

import com.vnpay.sit.config.SitBootstrapProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SitBootstrapProperties.class)
public class SitVnpayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SitVnpayApplication.class, args);
    }
}
