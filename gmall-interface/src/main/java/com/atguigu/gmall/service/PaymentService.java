package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

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
}
