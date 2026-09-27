package com.zhuyicheng.hoops.config;

import com.zhuyicheng.hoops.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 声明这是一个配置类
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 排除注册、登录接口，同时排除上传图片的访问路径
                .excludePathPatterns("/user/login", "/user/register", "/uploads/**","/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录下的 uploads 文件夹的绝对路径
        String localPath = System.getProperty("user.dir") + "/uploads/";

        // 映射关系：当访问 /uploads/** 时，去本地的 uploads/ 目录下找物理文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + localPath);
    }
}