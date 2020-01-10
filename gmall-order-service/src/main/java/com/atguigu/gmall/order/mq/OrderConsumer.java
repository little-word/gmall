package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * 消息队列
 *
 * @author GPX
 * @date 2020/1/10 17:47
 */
@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    /**
     * 消费者
     *
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE", containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {

//        MapMessage mapMessage = new ActiveMQMapMessage();
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println("result = " + result);
        System.out.println("orderId = " + orderId);
        //支付成功
        if ("success".equals(result)) {
            // 更新支付状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 通知减库存 更新完库存 更新订单状态 ("已发货"）
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }
}
