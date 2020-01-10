package com.atguigu.gmall.bean;

import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.PaymentWay;
import com.atguigu.gmall.enums.ProcessStatus;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author GPX
 * @date 2020/1/8 16:14
 */
@Data
public class OrderInfo implements Serializable {

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column//收货人名称
    private String consignee;

    @Column//收货人电话
    private String consigneeTel;


    @Column
    private BigDecimal totalAmount;

    @Column//订单状态-配送中/未配送 订单状态，用于显示给用户查看。设定初始值
    private OrderStatus orderStatus;
    @Column//订单进程 -订单进度状态，程序控制、 后台管理查看。设定初始值
    private ProcessStatus processStatus;

    @Column
    private String userId;

    @Column //支付方式
    private PaymentWay paymentWay;

    @Column //订单超时时间 默认当前时间+1天
    private Date expireTime;

    @Column//配送地址
    private String deliveryAddress;

    @Column//订单备注
    private String orderComment;

    @Column //订单创建时间
    private Date createTime;

    @Column
    private String parentOrderId;

    @Column//物流编号
    private String trackingNo;


    @Transient
    private List<OrderDetail> orderDetailList;


    @Transient
    private String wareId;

    @Column//第三方支付编号
    private String outTradeNo;

    public void sumTotalAmount() {
        //计算结算的总金额
        BigDecimal totalAmount = new BigDecimal("0");
        for (OrderDetail orderDetail : orderDetailList) {
            //multiply 乘
            totalAmount = totalAmount.add(orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum())));
        }
        this.totalAmount = totalAmount;
    }
}
