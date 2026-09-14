package com.samosajunction;

import com.samosajunction.assistant.config.AssistantProperties;
import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.security.JwtProperties;
import com.samosajunction.cart.config.CartProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, CartProperties.class, AuthProperties.class, AssistantProperties.class})
public class SamosaJunctionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SamosaJunctionApplication.class, args);
    }
}
