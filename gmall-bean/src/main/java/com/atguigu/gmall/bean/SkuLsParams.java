package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author GPX
 * @date 2020/1/3 19:02
 */
@Data
//DSL语句参数  特定的可以进行排序的查询语句
public class SkuLsParams implements Serializable{

    //skuName 中文分词查询的属性值
    String  keyword;

    String catalog3Id;

    //skuAttrValueList 中的属性
    String[] valueId;


    //分页  pageNo 起始位置  pageSize 每页数据
    int pageNo=1;

    int pageSize=2;
}
