package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author GPX
 * @date 2020/1/3 19:11
 */
@Data
//返回的结果集
public class SkuLsResult implements Serializable {

    // 属性： id,price,skuName,catalog3Id,skuDefaultImg,skuAttrValueList
    List<SkuLsInfo> skuLsInfoList;

    //        "hits":
//        "total":2,
//            "max_score":null,
//            "hits
    //查询到的（命中）数据
    long total;

    //总页数
    long totalPages;

    //返回 skuAttrValueList 中valeuId的集合 "catalog3Id": "61",---》属性值Id  attrValueIdList{83,82}
    List<String> attrValueIdList;
}
