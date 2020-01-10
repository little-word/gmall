package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService  {
    /**
     * 保存订单
     * 用户的订单数据 包含每条订单的数据 ，订单的状态等详细信息 --> 送货清单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号 解决用户利用浏览器回退重复提交订单
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 验证流水号
     * @param userId
     * @param tradeNo
     * @return
     */
    boolean checkTradeCode(String userId, String tradeNo);

    /**
     * 删除流水号
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 校验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 获取订单详情
     * 获取订单总金额 用于支付
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 收到消息后 更新订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 更新库存 更改订单状态 已发货
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 处理过期订单接口
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * 用于分布式事务 拆单使用
     * 单独只针对outTradeNo,id 查询
     * @param orderInfo
     * @return
     */
    OrderInfo getOrderInfo(OrderInfo orderInfo);

    /**
     * 拆单初始化订单
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo);

    /***
     * 拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
