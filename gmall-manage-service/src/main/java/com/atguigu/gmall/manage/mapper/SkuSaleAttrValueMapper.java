package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    /**
     * 点击其他销售属性值的组合，跳转到另外的sku页面 方式二
     * @param spuId
     * @return
     */
//    List<Map> getSaleAttrValuesBySpu(String spuId);

    /**
     * 点击其他销售属性值的组合，跳转到另外的sku页面 方式一
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}