package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author GPX
 * @date 2019/12/25 12:58
 */
@RestController
public class UserInfoController {

    @Autowired
    private UserService userService;

    @RequestMapping("findAll")
    public List<UserInfo>  findAll(){
        return  userService.findAll();
    }

}
