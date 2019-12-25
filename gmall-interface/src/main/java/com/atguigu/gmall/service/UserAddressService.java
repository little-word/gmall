package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserAddress;

import java.util.List;

public interface UserAddressService {

    //查询地址
    List<UserAddress> getUserAddressByUserId(String userId);
}
