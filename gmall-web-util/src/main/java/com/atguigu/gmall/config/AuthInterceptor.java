package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author GPX
 * @date 2020/1/6 19:37
 */
@Component
//权限控制 拦截器
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override//方法前拦截   true 放行 执行 方法------- false 进行拦截
//    有权限应返回 true 放行,执行 controller  每次访问控制器 都先执行拦截器中的方法
//    没有权限返回 false,拒绝后续执行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //index 页面拼接 传来的
        String token = request.getParameter("newToken");
        if (token != null) {
            //保存到cookies中  在任何控制器中都可以获得到token
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);

        }
        if (token == null) {
            //在不同控制器中获取到token 判断登录状态 一处登录 处处登录 其他业务页面会携带cookie
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        //解析token token真正存在 就是在当前页面登录的 会携带token http://www.gmall.com/?newToken=eyJhbG......
        if (token != null) {
            //读取token 获取nickName
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");

            //保存到request中 用于昵称 回显
            request.setAttribute("nickName", nickName);
        }
        //sso 单点登录拦截 在拦截器中获取方法的注解 LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire loginRequireAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);


        //判断 是否需要跳转到登录页面  默认 true 拦截--跳转 需要重定向
        if (loginRequireAnnotation != null) {
            //192.168.126.1  判断用户是否 直接登录 cookie 是否存储信息
            String remoteAddr = request.getHeader("x-forwarded-for");
            //"&currentIp="  "&salt="  远程调用http://passport.atguigu.com/verify  verify方法返回值success
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIp=" + remoteAddr);
            //校验的回显值 verify  已经登录 不需要跳转
            if ("success".equals(result)) {
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId", userId);
                return true;
            } else {
                //判断 是否需要登录 LoginRequire 为 false 不需要登录
                if (loginRequireAnnotation.autoRedirect()) {
                    //获取当前拦截的地址 http://item.gmall.com/43.html
                    String requestURL = request.getRequestURL().toString();

                    //跳转需要携带 originUrl
                    //encodeURL=http%3A%2F%2Fitem.gmall.com%2F43.html
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");

                    //跳转到登录页面 拼接地址跳转到当前页面 http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F43.html
                    response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeURL);
                    return false;
                }
            }
        }
        //获取到了token 获取到用户信息 不进行拦截
        return true;

    }

    /**
     * 解析token
     * 获取用户信息
     *
     * @param token
     * @return
     */
    private Map getUserMapByToken(String token) {

        //获取token 的中间值  http://item.gmall.com/43.html?
        // newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IuWwj-aYjiIsInVzZXJJZCI6IjEifQ.CAGkkxQroNcIlUqo9WlimOM54Cla9mDhUjJcSCTRn0c
        String tokenUserInfo = StringUtils.substringBetween(token, ".");

        //解码  在JWTUtil中加密的-->JwtUtil.encode
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);

        String tokenJson = null;
        try {
            //设置统一编码
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;
    }



}
