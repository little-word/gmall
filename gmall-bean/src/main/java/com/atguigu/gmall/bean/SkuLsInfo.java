package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author GPX
 * @date 2020/1/3 12:48
 */
@Data
public class SkuLsInfo implements Serializable {

    String id;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;

    Long hotScore=0L;

    //List<SkuAttrValue> skuAttrValueList; 与skuInfo 的属性一致 添加时作为key 来保存数据
//    skuAttrValueList": [
//    {
//             "id": "39",
//            "attrId": "23",
//            "valueId": "83",
//            "skuId": "21"
//    },
    List<SkuLsAttrValue> skuAttrValueList;
}
