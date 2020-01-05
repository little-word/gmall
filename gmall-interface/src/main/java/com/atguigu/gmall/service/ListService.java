package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {
    /**
     * 保存SkuInfo到es中
     * @param skuLsInfo
     * @return
     */
    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * 编写dsl查询语句
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 热点数据排序 hotScore 在 skuLsinfo 类中
     * @param skuId
     * @return
     */
    String incrHotScore(String skuId);
}
