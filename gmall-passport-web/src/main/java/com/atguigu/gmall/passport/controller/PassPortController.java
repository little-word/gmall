package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GPX
 * @date 2020/1/5 17:44
 */
@Controller
@CrossOrigin
public class PassPortController {

    @Value("${token.key}")
    String signKey;

    @Reference
    private UserService userService;

    @RequestMapping("/index")
    public String index(HttpServletRequest request) {

        //http://passport.atguigu.com/index?originUrl=https%3A%2F%2Fwww.jd.com%2F
        String originUrl = request.getParameter("originUrl");
        // 保存 登录后跳转到当前 的页面
        request.setAttribute("originUrl", originUrl);

        System.out.println("originUrl:"+originUrl);
        return "index";
    }

    /**
     * 需要携带用户名 用户密码 用户id
     *
     * @return
     */
    @RequestMapping(value = "/login")
    @ResponseBody
    public String userLogin(UserInfo userInfo, HttpServletRequest request) {

        //获取连接地址 192.168.126.1
        String salt = request.getHeader("X-forwarded-for");
        if (userInfo != null) {
            UserInfo info = userService.login(userInfo);
            if (info != null) {
            }
            //生成Token
            Map map = new HashMap();
            map.put("userId", info.getId());
            map.put("nickName", info.getNickName());

            String token = JwtUtil.encode(signKey, map, salt); //newToken
            System.out.println("token:" + token);
            return token;
        } else {
            return "fail";
        }
    }

    //校验 用户 登陆信息 登陆/注册

    @RequestMapping("/verify")  //verify 校验
    @ResponseBody
    public String verify(HttpServletRequest request) {

//        String salt = request.getParameter("salt");
        String currentIp = request.getParameter("currentIp");
        String token = request.getParameter("token");

        //检查Token
        Map<String, Object> map = JwtUtil.decode(token, signKey, currentIp);
        if (map != null) {
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if (userInfo != null) {
                return "success";
            }
        }
        return "fail";
    }
}


