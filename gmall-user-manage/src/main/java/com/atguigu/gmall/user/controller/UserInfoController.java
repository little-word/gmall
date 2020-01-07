package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author GPX
 * @date 2019/12/25 12:58
 */
@RestController
public class UserInfoController {

    @Reference
    private UserService userService;

    @RequestMapping("findAll")
    public List<UserInfo>  findAll(){
        return  userService.findAll();
    }

}
