package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.config.WebConst;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author GPX
 * @date 2020/1/7 13:06
 */
@Controller
@CrossOrigin
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;
    /**
     * 业务分析
     * 1、根据skuId查询出商品详情skuInfo
     2、把skuInfo信息对应保存到购物车[购物车的实体类]
     3、返回成功页面
     */

    /**
     * 添加购物车 要有登录拦截
     *
     * @param request
     * @param response
     * @return
     * @LoginRequire 自定义注解--web-util
     */
    @RequestMapping("/addToCart")
    @LoginRequire(autoRedirect = false)//自动跳转 取消
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {

        //商品详情页面携带过来的 item.html 页面中 itemForm<input type="text" name="skuNum" id="" value="1" />
        String skuId = request.getParameter("skuId");
        //购买的数量 用于添加成功 页面展示数据
        String skuNum = request.getParameter("skuNum");
        //  request.setAttribute("userId", userId);  AuthInterceptor拦截器 中设置的 用于校验
        String userId = (String) request.getAttribute("userId");
        //判断userId是否存在 存在 已登录
        if (userId == null) {
            //判断cookie中是否存在 可能存在 上次未登录添加生成的临时Id
            userId = CookieUtil.getCookieValue(request, "my-userId", false);
            // 如果cookie 中没有userId,则新建一个userId,并放入cookie中  临时userId
            if (userId == null) {
                //临时id存放到数据库 区别加入购物车的数据信息 ---合并购物车使用 判断合并人的信息------> 数据库可以不保存 未登录状态下的购物车
                userId = UUID.randomUUID().toString().replaceAll("-", "");
                CookieUtil.setCookie(request, response, "my-userId", userId, WebConst.COOKIE_MAXAGE, false);
            }
        }
        //加入购物车
        cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));

        //添加的是商品详情 保存商品详情
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);

        //保存添加的数量
        request.setAttribute("skuNum", skuNum);
        return "success";
    }

    /**
     * 购买后跳转到 展示购物车列表
     */
    @RequestMapping("/cartList")
    @LoginRequire(autoRedirect = false)//展示购物车商品 需要跳转到登录页面这里避免测试麻烦设置为false
    public String cartList(HttpServletRequest request, HttpServletResponse response) {

        //获取userId
        String userId = (String) request.getAttribute("userId");


        //通过userId 获取 商品详情 未合并
//        List<CartInfo> cartInfoList = new ArrayList<>();
//        if (userId != null) {
//            //用户登录 获取详情
//            cartInfoList = cartService.getCartList(userId);
//        } else {
//            //没登录 获取cookie中的my-userId  加入购物车时设置的 判断未登录时有没有添加到购物车的商品
//            String userTempId = CookieUtil.getCookieValue(request, "my-userId", false);
//
//            if (userTempId != null) {
//                cartInfoList = cartService.getCartList(userTempId);
//            }
//        }

        //合并购物车redis中的 和数据库的 合并

        // 购物车集合列表 cartInfoList
        List<CartInfo> cartInfoList = null;
        if (userId == null) { //在别的页面登录 拦截器 设置的  查询临时id 缓存的数据
            //直接访问 购物车页面展示购物车详情
            //从未登录的购物车（redis）获取数据
            //redis key=user:userId:cart 从cookie 中获取临时的userId
            String userTempId = CookieUtil.getCookieValue(request, "my-userId", false);
            //调用服务层的方法获取缓存中的数据 获取到临时id
            if (!StringUtils.isEmpty(userTempId)) {
                // 从缓存中获取购物车数据列表 临时id:ddbed2fc03a143368fc8709e51c65ceb
                cartInfoList = cartService.getCartList(userTempId);
            }
        } else {
            //从别的页面跳转到购物车页面 携带有userid 在拦截器传入的
            // 查询未登录是否有购物车数据
            // 从cookie 中获取临时的userId
            String userTempId = CookieUtil.getCookieValue(request, "my-userId", false);

            // 调用服务层的方法获取缓存中的数据 合并购物车{合并未登录购物车数据}

            // 声明一个集合来存储未登录数据
            List<CartInfo> cartTempList = new ArrayList<>();
            if (!StringUtils.isEmpty(userTempId)) {
                //合并购物车 cartTempList未登录购物车，根据userId 查询登录购物车

                //从数据库中获取 存储的未登录的数据临时id
                cartTempList = cartService.getCartList(userTempId);

                if (cartTempList != null && cartTempList.size() > 0) {
                    //开始合并 合并未登录的购物车数据
                    cartInfoList = cartService.mergeToCartList(cartTempList, userId);
                    // 合并后 删除未登录购物车数据--数据库中临时id的数据
                    cartService.deleteCartList(userTempId);
                }
            }
            if (userTempId == null || (cartTempList == null || cartTempList.size() == 0)) {
                // 说明未登录reids没有数据， 直接获取数据库！
                cartInfoList = cartService.getCartList(userId);
            }
        }

        //将结果存放到域中
        request.setAttribute("cartInfoList", cartInfoList);
        return "cartList";

    }

    /**
     * 选中状态的变更
     * 选中 去结算
     *
     * @param request
     * @param response
     */
    @RequestMapping("/checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = true)
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {
        //调用服务层  页面展示的数据==isCheckedFlag="1";
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        //获取用户id
        String userId = (String) request.getAttribute("userId");

        // 判断用户的状态！
        if (userId == null) {
            // 在其他页面登录
            userId = CookieUtil.getCookieValue(request, "my-userId", false);
        }
        cartService.checkCart(isChecked, skuId, userId);
    }

    /**
     * 去结算 必须登录
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {

        //获取userid
        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cartTempList = null;
        // 获取cookie 中的my-userId 未登录redis中选中的数据
        String userTempId = CookieUtil.getCookieValue(request, "my-userId", false);
        if (userTempId != null) {
            cartTempList = cartService.getCartList(userTempId);
            //缓存中有数据
            if (cartTempList != null && cartTempList.size() > 0) {
                // 合并勾选状态 并删除 结算的商品-->结算
                cartService.mergeToCartList(cartTempList, userId);
                //删除数据库中 临时id存储的数据--缓存中的商品
                cartService.deleteCartList(userTempId);
            }
        }
        return "redirect://trade.gmall.com/trade";
    }
}
