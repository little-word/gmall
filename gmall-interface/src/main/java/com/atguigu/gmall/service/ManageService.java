package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

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

//    选中准修改数据 ， 根据该attrId 去查找AttrInfo，该对象下 List<BaseAttrValue> ！
//    所以在返回的时候，需要返回BaseAttrInfo。
    /**
     * 修改商品属性值（完整）
     */
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     * 修改商品属性值--不完整
     */
  //  List<BaseAttrValue> getAttrValueList(String attrId);
}
