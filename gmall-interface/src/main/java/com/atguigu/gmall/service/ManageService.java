package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;
import java.util.Map;

public interface ManageService {
    /**
     * 获取所有一级分类
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类ID获取二级分类
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);
    //方法的重载 功能 一样 代码更加简洁
    List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2);

    /**
     * 根据二级分类ID获取三级分类
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 根据三级分类id获取平台属性
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     *
     * 添加商品属性两张表
     */
    void  saveAttrInfo(BaseAttrInfo baseAttrInfo);
    /**
     * 修改商品属性值--不完整
     */
    //  List<BaseAttrValue> getAttrValueList(String attrId);

//    选中准修改数据 ， 根据该attrId 去查找AttrInfo，该对象下 List<BaseAttrValue> ！
//    所以在返回的时候，需要返回BaseAttrInfo。
    /**
     * 修改商品属性值（完整）
     */
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     * 获取商品的Spu  展示详情
     *   List<SpuInfo> getSpuInfoList(String catalog3Id);
     * @param
     * @return
     */
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    /**
     * 回显销售属性
     * @param
     */
    List<BaseSaleAttr> baseSaleAttrList();

    /**
     * 添加销售属性
     * @param spuInfo
     */

    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * sku显示图片信息
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * SKU显示商品销售属性
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    /**
     * 保存SKU数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 静态页面展示sku详情
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 前端展示 平台销售属性
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 点击其他销售属性值的组合，跳转到另外的sku页面 方式二
     * @param spuId
     * @return
     */
//    Map getSkuValueIdsMap(String spuId);

    /**
     * 点击其他销售属性值的组合，跳转到另外的sku页面 方式一
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);
}
