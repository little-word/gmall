package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

/**
 * 消息队列测试 生产者
 * @author GPX
 * @date 2020/1/10 16:38
 */
public class ProducerTest {
    public static void main(String[] args) throws JMSException {
        //创建连接工厂
        ActiveMQConnectionFactory connectionFactory  = new ActiveMQConnectionFactory("tcp://192.168.126.129:61616");
        Connection connection = connectionFactory.createConnection();
        connection.start();
        // 创建session 第一个参数表示是否支持事务，false时，第二个参数Session.AUTO_ACKNOWLEDGE，Session.CLIENT_ACKNOWLEDGE，DUPS_OK_ACKNOWLEDGE其中一个
        // 第一个参数设置为true时，第二个参数可以忽略 服务器设置为SESSION_TRANSACTED
        //AUTO_ACKNOWLEDGE自动确认模式，一旦接收方应用程序的消息处理回调函数返回
        //Session.CLIENT_ACKNOWLEDGE 手动签收确认 消息
        //允许消息重复 Session.DUPS_OK_ACKNOWLEDGE
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //创建队列
        Queue queue = session.createQueue("Atguigu");

        //创建生产者
        MessageProducer producer = session.createProducer(queue);

        //创建消息对象
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("Hello ActiveMq");
        //发送消息
        producer.send(activeMQTextMessage);
        producer.close();
        connection.close();
    }
}
