package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author GPX
 * @date 2019/12/25 18:16
 */
@Data
public class BaseAttrValue implements Serializable{
    @Id
    @Column
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;

    @Transient
    private String urlParam;

}
