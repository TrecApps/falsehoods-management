package com.trecapps.falsehoods;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.trecapps.falsehoods.*",                     // Scan this app
        "com.trecauth.common.*",               // Authentication library
        "com.trecauth.webflux.*"
}
)
@EnableWebFlux
@Configuration
public class Driver {
    public static void main(String[] args)
    {
        ApplicationInsights.attach();
        SpringApplication.run(Driver.class, args);
    }



}
