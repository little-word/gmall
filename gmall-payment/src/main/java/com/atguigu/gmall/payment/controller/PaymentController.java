package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GPX
 * @date 2020/1/9 13:25
 */
@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;
    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * TODO 支付宝开发手册
     * https://docs.open.alipay.com/270/105902/
     */


    @RequestMapping("/index")
    @LoginRequire
    public String index(HttpServletRequest request, String orderId) {

        request.setAttribute("orderId", orderId);

        //获取总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        //传入商品总价
        request.setAttribute("totalAmount", totalAmount);
        return "index";
    }

    /**
     * 数据在在saveOrder设置
     * 提交支付
     * alipay/submit保存支付信息
     *
     * @return
     */
    @RequestMapping(value = "/alipay/submit", method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response) {
        String orderId = request.getParameter("orderId");
        //获取订单信息 保存支付信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        PaymentInfo paymentInfo = new PaymentInfo();

        //TODO 第三方支付编号 在saveOrder方法中生成设置的
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        //交易内容。利用商品名称拼接  订单标题
        paymentInfo.setSubject("支付宝支付测试");
        paymentInfo.setCreateTime(new Date());
        //订单的状态---支付中
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);

        //保存支付信息
        paymentService.savePaymentInfo(paymentInfo);


        // 支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        //放入对应的参数 支付回跳地址 同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_order_url);
        //异步回调  通知地址 支付宝服务器主动通知商户服务
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        // 声明一个Map 用于保存支付宝的各种参数 业务请求参数  不需要对json字符串转义
        Map<String, Object> bizContnetMap = new HashMap<>();
        bizContnetMap.put("out_trade_no", paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject", paymentInfo.getSubject());
        bizContnetMap.put("total_amount", paymentInfo.getTotalAmount());
        // 将map变成json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //发生的文本格式
        response.setContentType("text/html;charset=UTF-8");
        return form;
    }

    /**
     * 这里requestMapping对应的路径必须与之前传给支付宝的AlipayConfig.return_order_url保持一致。
     * 同步通知:用于用户在支付宝页面付款完毕后自动跳转；支付成功了--不一定扣款
     * return_payment_url=http://payment.gmall.com/alipay/callback/return
     * callbackReturn
     * 同步回调
     */
    @RequestMapping("/alipay/callback/return")
    public String callbackReturn() {
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 异步是服务器在后端处理支付成功或失败时的业务逻辑
     * 确认并记录用户已付款，通知电商模块
     * 1、验证回调信息的真伪
     * 2、验证用户付款的成功与否
     * 3、把新的支付状态写入支付信息表{paymentInfo}中。
     * 4、通知电商
     * 给支付宝返回回执
     * 在后台真正--付款或付款失败处理
     * <p>
     * notify_payment_url=http://60.205.215.91/alipay/callback/notify
     */
    @RequestMapping(value = "/alipay/callback/notify", method = RequestMethod.POST)
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String, String> paramMap, HttpServletRequest request) throws AlipayApiException {

        //异步通知验签：
        //Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
//        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, ALIPAY_PUBLIC_KEY, CHARSET, SIGN_TYPE) //调用SDK验证签名
//        if(signVerified){
//            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
//        }else{
//            // TODO 验签失败则记录异常日志，并在response中返回failure.
//        }
        //签名类型 RSA2 签名方式 调用SDK 签名 alipay自己的 方法
        boolean flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);

        if (!flag) {
            //支付失败
            return "fail";
        }
        // 判断结束  alipay 页面自己设置的
        String trade_status = paramMap.get("trade_status");
        if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
            // 查单据是否处理 第三方支付流水号  在OrderServiceImpl saveOrder 方法中传入页面的
            String out_trade_no = paramMap.get("out_trade_no");

            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOutTradeNo(out_trade_no);

            //获取订单支付状态paymentInfo
            PaymentInfo paymentInfoHas = paymentService.getPaymentInfo(paymentInfo);

            //枚举类PaymentStatus 在提交订单时设置的submitOrder--》ordercontroller中
            //  初始化参数 未支付 orderInfo.setOrderStatus(OrderStatus.UNPAID);orderInfo.setProcessStatus(ProcessStatus.UNPAID);
            //这里是已支付/订单关闭
            if (paymentInfoHas.getPaymentStatus() == PaymentStatus.PAID || paymentInfoHas.getPaymentStatus() == PaymentStatus.ClOSED) {
                return "fail";
            } else {
                //支付成功后 修改 订单状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                // 设置状态
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                // 设置创建时间
                paymentInfoUpd.setCallbackTime(new Date());
                // 设置内容
                paymentInfoUpd.setCallbackContent(paramMap.toString());
                //支付成功 更新订单状态
                paymentService.updatePaymentInfo(out_trade_no, paymentInfoUpd);
                return "success";
            }
        }
        return "fail";
    }

    /**
     * 直接在浏览器发起请求即可！
     * http://payment.gmall.com/refund?orderId=98
     * 退款
     */

    @RequestMapping("/refund")
    @ResponseBody
    public String refund(String orderId) {
        boolean flag = paymentService.refund(orderId);
        System.out.println("flag:" + flag);
        return flag + "";
    }
}
