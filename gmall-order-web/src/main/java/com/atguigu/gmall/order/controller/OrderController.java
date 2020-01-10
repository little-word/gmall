package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GPX
 * @date 2019/12/25 16:07
 */
@Controller
public class OrderController {


    //这里产生了跨域调用 这个错误 不影响运行 调用了userService 不同的项目
    // @Autowired //在mapper中映射 default=false 忽略查询
    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;


    //    @RequestMapping("/trade")
//    public List<UserAddress> trade(String userId){
//
//        return userService.getUserAddressByUserId(userId);
//    }
//    orderInfo:订单表
//    orderDetail:订单明细表

    /**
     * 初始化订单号
     * @param request
     * @return
     */
    @RequestMapping("/trade")
    @LoginRequire(autoRedirect = false)
    public String tradeInit(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        //用户地址
        List<UserAddress> userAddressList = userService.getUserAddressByUserId(userId);

        //页面数据---收货人信息 收件人xxx 具体地址xx
        request.setAttribute("userAddressList", userAddressList);

        // 订单信息集合 获取购物车选中的物品
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);

        //购买商品的集合
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        for (CartInfo cartInfo : cartCheckedList) {
            //每件商品的信息 订单明细数据
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());

            //将每件商品详情加入 购买商品的集合
            orderDetailList.add(orderDetail);
        }

        //获取TradeCode号 流水号 增加流水号的生成  解决用户重复提交
        // 利用浏览器回退重复提交订单
//        在进入结算页面时，生成一个结算流水号，然后保存到结算页面的隐藏元素中，
//        每次用户提交都检查该流水号与页面提交的是否相符，订单保存以后把后台的流水号删除掉。
//        那么第二次用户用同一个页面提交的话流水号就会匹配失败，无法重复保存订单。

        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeCode", tradeNo);
        request.setAttribute("orderDetailList", orderDetailList);

        //用户的订单数据 包含每条订单的数据 ，订单的状态等详细信息 --> 送货清单 用于计算总价格
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        //计算总价格
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        return "trade";
    }

    /**
     * 提交订单 避免重复提交
     * 验证流水号是否存在
     * tradeCode
     *
     * @param orderInfo
     * @param request
     * @return
     */
    @RequestMapping("/submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request) {
        //获取userId
        String userId = (String) request.getAttribute("userId");
        //检查tradeCode
        String tradeNo = request.getParameter("tradeNo");
        //校验流水号 订单是否已提交
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }


        //初始化参数 未支付
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);

        // 校验，验价  校验库存 OrderDetail单个订单的详细信息 表
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 从订单中去购物skuId，数量 //验证库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result) {
                request.setAttribute("errMsg", "商品库存不足，请重新下单！");
                return "tradeFail";
            }
            //验证价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int price = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if (price != 0){
                request.setAttribute("errMsg", orderDetail.getOrderPrice()+"商品价格不匹配！");
                //重新查询价格 查询实时价格 并更新redis
                cartService.loadCartCache(userId);
                return "tradeFail";
            }

        }
            // 保存订单状态
        String orderId = orderService.saveOrder(orderInfo);

        // 删除tradeNo 删除流水号避免重复提交
        orderService.delTradeNo(userId);
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }
}
