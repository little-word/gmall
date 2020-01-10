package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

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
}
