package com.atguigu.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * 接收延迟队列的消费端
 * 拆单
 * @author GPX
 * @date 2020/1/10 19:15
 */
@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;
    @Reference
    private OrderService orderService;
    // 监听消息队列 PAYMENT_RESULT_CHECK_QUEUE paymentServiceImpl 方法closeOrderInfo 中创建的 队列
    //jmsQueueListener 在serviceUtil activeMQConfig 配置类中 DefaultJmsListenerContainerFactory
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerCheckQueue(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("outTradeNo");

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        // PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        // 检查该用户是否支付成功？
        boolean result = paymentService.checkPayment(orderInfo);
        System.out.println("支付结果："+result);
        if (!result){
            //  再次检查是否支付成功！
            // paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);

            // 关闭过期订单 处理过期订单接口
            OrderInfo orderInfoQuery = orderService.getOrderInfo(orderInfo);
            orderService.execExpiredOrder(orderInfoQuery);
            System.out.println("订单已经关闭！");
        }
    }
}
