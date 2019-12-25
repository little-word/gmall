package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author GPX
 * @date 2019/12/25 16:07
 */
@RestController
public class OrderController {


    //这里产生了跨域 调用 这个错误 不影响运行 调用了userService 不同的项目
   // @Autowired //在mapper中映射 default=false 忽略查询
    @Reference
    private UserService userService;


    @RequestMapping("trade")
    public List<UserAddress> trade(String userId){

        return userService.getUserAddressByUserId(userId);
    }
}
