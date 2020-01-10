package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author GPX
 * @date 2020/1/6 20:06
 */
//拦截器配置文件
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private AuthInterceptor authInterceptor;

    // 定义一个拦截器，继承成springmvc的HandlerInterceptorAdapter
    //通过重写它的preHandle方法实现，业务代码前的校验工作
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //拦截authInterceptor
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
        super.addInterceptors(registry);
    }
}
