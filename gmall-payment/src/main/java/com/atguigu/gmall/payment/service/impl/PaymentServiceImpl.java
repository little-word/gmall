package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;

/**
 * @author GPX
 * @date 2020/1/9 14:16
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 保存支付信息
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insert(paymentInfo);
    }

    /**
     * 获取订单支付状态
     *
     * @param paymentInfo
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    /**
     * 支付成功 更新订单状态
     *
     * @param outTradeNo  第三方支付流水号  在OrderServiceImpl saveOrder 方法中传入页面的
     * @param paymentInfo 订单的状态
     */
    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", outTradeNo);
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    /**
     * 退款
     *
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(String orderId) {
//        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", "", "", "json", "GBK", AlipayConfig.alipay_public_key, "RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        PaymentInfo paymentInfo = getPaymentInfoByOrderId(orderId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("refund_amount", paymentInfo.getTotalAmount());

        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    private PaymentInfo getPaymentInfoByOrderId(String orderId) {

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        return paymentInfoMapper.selectOneByExample(example);
    }

}
