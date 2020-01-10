package com.atguigu.gmall.bean;

import com.atguigu.gmall.enums.PaymentStatus;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author GPX
 * @date 2020/1/9 14:09
 */
@Data
public class PaymentInfo implements Serializable {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String  id;

    @Column//在orderinfo 中 在OrderServiceImpl saveOrder 方法中传入页面的
    private String outTradeNo;

    @Column
    private String orderId;

    @Column
    private String alipayTradeNo;

    @Column
    private BigDecimal totalAmount;

    @Column
    private String Subject;

    @Column
    private PaymentStatus paymentStatus;

    @Column
    private Date createTime;

    @Column
    private Date callbackTime;

    @Column
    private String callbackContent;
}
