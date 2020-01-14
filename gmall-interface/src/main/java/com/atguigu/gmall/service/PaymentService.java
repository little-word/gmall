package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存支付信息
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 获取订单支付状态
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 支付成功 更新订单状态
     * @param out_trade_no
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 微信支付
     * @param orderId
     * @param totalAmount
     * @return
     */
    Map createNative(String orderId, String totalAmount);

    /**
     * 发送支付结果给订单
     *消息中间件 消息队列的加入
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 延迟队列 实现支付宝订单状态查询
     * 拆单
     * @param orderInfo
     * @return
     */
    boolean checkPayment(OrderInfo orderInfo);

    /**
     * 发送延时队列
     * @param outTradeNo
     * @param delaySec 每隔多长时间查询一次
     */
    void closeOrderInfo(String outTradeNo,int delaySec);
}
