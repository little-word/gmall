package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author GPX
 * @date 2019/12/25 12:56
 */
@Service
public class UserServiceImpl implements UserService {

    public String userKey_prefix = "user:";
    public String userinfoKey_suffix = ":info";
    public int userKey_timeOut = 60 * 60 * 24;

    //    @Autowired(required = false)
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressByUserId(String userId) {
        Example example = new Example(UserAddress.class);
        example.createCriteria().andEqualTo("userId", userId);

        return userAddressMapper.selectByExample(example);
    }

    /**
     * 登录 and 判断
     *
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {

        //从数据库中查询
        UserInfo info = userInfoMapper.selectOne(userInfo);

        //给密码加密
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);
        //将登陆 信息保存到reids中

        if (info != null) {
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix + info.getId() + userinfoKey_suffix, userKey_timeOut, JSON.toJSONString(info));

            jedis.close();
            return info;
        }
        return null;
    }

    /**
     * 校验Token
     *
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {
        //获取 redis中的 数据
        Jedis jedis = redisUtil.getJedis();
        String key = userKey_prefix+ userId + userinfoKey_suffix;
        String userJson = jedis.get(key);

        //延长时间
        jedis.expire(key, userKey_timeOut);
        if (userJson != null) {
            UserInfo userInfo = JSON.parseObject(userJson,UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
