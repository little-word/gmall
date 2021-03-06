package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author GPX
 * @date 2019/12/25 16:11
 */
@Data
public class UserAddress implements Serializable {

    @Column
    @Id
    private String id;
    @Column
    private String userAddress;
    @Column
    private String userId;
    @Column //收件人
    private String consignee;
    @Column
    private String phoneNum;
    @Column
    private String isDefault;
}
