package com.teamame.smartproperties.web.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import com.teamame.smartproperties.api.SmartPropertiesApi;

@SpringBootApplication
public class SmartPropertiesWebDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPropertiesWebDemoApplication.class, args);
    }

    @Bean
    @Scope("singleton")
    public SmartPropertiesApi createSmartPropApi() {
        String apiUrl = System.getenv("SMART_PROPERTIES_API_URL");
        String tenant = System.getenv("SMART_PROPERTIES_TENANT");
        String workspace = System.getenv("SMART_PROPERTIES_WORKSPACE");
        String apiToken = System.getenv("SMART_PROPERTIES_API_TOKEN");

        if (apiUrl == null) apiUrl = "http://localhost:3001/api/v2";
        if (tenant == null) tenant = "ripleype";
        if (workspace == null) workspace = "prod";
        if (apiToken == null) apiToken = "0e0370c308cf0919d66a29034b3303f4";

        SmartPropertiesApi smartPropertiesApi = new SmartPropertiesApi(apiUrl, tenant, workspace, apiToken);
        
        smartPropertiesApi.initialize();
        return smartPropertiesApi;
    }

}
