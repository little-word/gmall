package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    /**
    添加到购物车
     */
    void addToCart(String skuId, String userId,Integer skuNum);

    /**
     * 根据userId 查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车 合并未登录的购物车数据
     * @param cartTempList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartTempList, String userId);

    /**
     * 合并后删除 数据库中 临时存放的商品 详情 根据临时id删除
     * @param userTempId
     */
    void deleteCartList(String userTempId);

    /**
     * 选中购物车 去结算使用
     * @param isChecked
     * @param skuId
     * @param userId
     */
    void checkCart(String isChecked, String skuId, String userId);

    /**
     * 获取购物车选中商品的集合
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 从数据库获得购物车详情 并加载到redis中
     * 通过UserId查询 实时价格
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
