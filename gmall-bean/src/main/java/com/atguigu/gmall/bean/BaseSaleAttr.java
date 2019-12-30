package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author GPX
 * @date 2019/12/28 16:04
 */
@Data
public class BaseSaleAttr implements Serializable{

    @Id
    @Column
    String id ;

    @Column
    String name;
}
