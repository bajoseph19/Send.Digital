package com.varscon.sendcorp.SendCorp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public Docket swaggerConfig(){
        return new Docket(DocumentationType.SWAGGER_2).select()
//                .paths(PathSelectors.ant("/api/*"))
//                .apis(RequestHandlerSelectors.basePackage("com.costomah"))
        .build().apiInfo(getApiInfo());
    }

    private ApiInfo getApiInfo(){
        return new ApiInfo(
                "SendCorp API",
                "Api for SendCorp fintech application",
                "1.0",
                "Contact the service company for more info",
                new Contact("Varscon Ltd", "https://varscongroup.com", "support@varscongroup.com"),
                "API License",
                "https://send2corp.com/api",
                Collections.emptyList()
        );
    }
}
