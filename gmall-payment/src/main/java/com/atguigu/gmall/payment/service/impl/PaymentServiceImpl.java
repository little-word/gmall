package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.util.HttpClient;
import com.atguigu.gmall.service.PaymentService;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GPX
 * @date 2020/1/9 14:16
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String mchId;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

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
     * alipay 开发文档 有方法
     *
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(String orderId) {
//  封装到alipayconfig 的alipayClient 中 已经在容器中了     AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", "", "", "json", "GBK", AlipayConfig.alipay_public_key, "RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //获取交易记录
        PaymentInfo paymentInfo = getPaymentInfoByOrderId(orderId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("refund_amount", paymentInfo.getTotalAmount());

        //生成二维码的 数据 订单的详细信息 生成的 在submitPayment 控制器中生成的
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            //TODO 退款成功 更改交易记录 订单状态
            PaymentInfo info = new PaymentInfo();
            info.setPaymentStatus(PaymentStatus.PAY_FAIL);
            updatePaymentInfo(paymentInfo.getOutTradeNo(), info);
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    private PaymentInfo getPaymentInfoByOrderId(String orderId) {

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId", orderId);
        return paymentInfoMapper.selectOneByExample(example);
    }

    /**
     * 微信支付
     *
     * @param orderId
     * @param total_fee 总金额数  totalAmount
     * @return
     */
    @Override
    public Map createNative(String orderId, String total_fee) {

        //1.创建参数
        Map<String, String> param = new HashMap();//创建参数
        param.put("appid", appid);//公众号
        param.put("mch_id", mchId);//商户号
        param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        param.put("body", "尚硅谷");//商品描述
        param.put("out_trade_no", orderId);//商户订单号
        param.put("total_fee", total_fee);//总金额（分）
        param.put("spbill_create_ip", "127.0.0.1");//IP
        //回调地址(随便写) 异步回调地址 内网穿透地址 http://gpx.free.idcfengye.com/alipay/callback/notify
        param.put("notify_url", " http://2z72m78296.wicp.vip/wx/callback/notify");
        param.put("trade_type", "NATIVE");//交易类型
        try {
            //2.生成要发送的xml  将map转换为xml
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println(xmlParam);
            //统一下单微信支付接口 将map发送到这个地址 。远程发送
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            //安全协议
            client.setHttps(true);

            //发送的参数-->数据
            client.setXmlParam(xmlParam);
            //发送方式
            client.post();
            //3.获得 支付结果 coude_url
            String result = client.getContent();
            System.out.println(result);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            Map<String, String> map = new HashMap<>();
            map.put("code_url", resultMap.get("code_url"));//支付地址
            map.put("total_fee", total_fee);//总金额
            map.put("out_trade_no", orderId);//订单号
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }

    }

    /**
     * 发送支付结果给订单
     * 消息队列的加入
     * 发送方法
     *
     * @param paymentInfo
     * @param result
     */
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            //开始事务 true
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //创建队列 生产者 创建消息  发送消息
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(paymentResultQueue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId", paymentInfo.getOrderId());
            mapMessage.setString("result", result);
            //发送
            producer.send(mapMessage);

            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改示例
     * 支付宝查询接口文档：https://docs.open.alipay.com/api_1/alipay.trade.query
     * 延迟队列  实现支付宝订单状态查询
     *
     * @param orderInfo
     * @return
     */
    @Override
    public boolean checkPayment(OrderInfo orderInfo) {
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // 判断当前对象不能为null！
        if (orderInfo == null) {
            return false;
        }

        // 设置查询的参数
        HashMap<String, String> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            // 得到交易状态！
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())) {
                System.out.println("支付成功！");
                return true;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
        return false;
    }

    /**
     * 发送延迟队列
     *
     * @param outTradeNo
     * @param delaySec   每隔多长时间查询一次
     */
    @Override
    public void closeOrderInfo(String outTradeNo, int delaySec) {
        // 创建工厂
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建对象
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            // 创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo", outTradeNo);

            // 开启延迟队列的参数设置
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySec * 1000);
            // 发送消息
            producer.send(activeMQMapMessage);

            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
