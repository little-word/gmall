package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

//业务层接口
public interface UserService {
    //查询所有
    List<UserInfo> findAll();

    /**
     *
     * 根据用户id查询用胡地址
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressByUserId(String userId);
}
