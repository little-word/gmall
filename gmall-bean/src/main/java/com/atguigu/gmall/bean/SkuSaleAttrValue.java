package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author GPX
 * @date 2019/12/29 16:35
 */
@Data
public class SkuSaleAttrValue implements Serializable{
    @Id
    @Column
    String id;

    @Column
    String skuId;

    @Column
    String saleAttrId;

    @Column
    String saleAttrValueId;

    @Column
    String saleAttrName;

    @Column
    String saleAttrValueName;

    //用于前端页面的 并关联某skuid如果能关联上is_check设为1，否则设为0。
    @Transient
    String isChecked;

}
