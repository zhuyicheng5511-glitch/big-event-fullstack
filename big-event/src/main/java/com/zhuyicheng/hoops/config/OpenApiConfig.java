package com.zhuyicheng.hoops.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hoopsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hoops 篮球数据统计平台")
                        .description("篮球赛事数据管理：球队 / 球员 / 赛季 / 比赛 / 单场数据 / 投篮事件")
                        .version("1.0.0"));
    }
}
