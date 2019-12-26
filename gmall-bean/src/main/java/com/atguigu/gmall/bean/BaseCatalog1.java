package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * 一级分类
 * @author GPX
 * @date 2019/12/25 18:12
 */
@Data
public class BaseCatalog1 implements Serializable{
    @Id
    @Column
    private String id;
    @Column
    private String name;
}
