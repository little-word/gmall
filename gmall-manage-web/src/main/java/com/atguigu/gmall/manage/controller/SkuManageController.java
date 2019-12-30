package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * @author GPX
 * @date 2019/12/29 13:28
 */
@RestController
@CrossOrigin
public class SkuManageController {


    @Reference
    private ManageService manageService;

    /**
     * sku显示图片信息
     * http://localhost:8082/spuImageList?spuId=5
     *
     * @param spuImage
     * @return
     */
    @RequestMapping("/spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {

        return manageService.getSpuImageList(spuImage);
    }

    /**
     * http://localhost:8082/spuSaleAttrList?spuId=72
     * 展示SKU商品销售属性
     * @param spuId
     * @return
     */
    @RequestMapping("/spuSaleAttrList")
    public  List<SpuSaleAttr> getspuSaleAttrList(String spuId){

        return manageService.spuSaleAttrList(spuId);
    }

    /**
     * http://localhost:8082/saveSkuInfo
     * 保存商品SKU四张表
     */
    @RequestMapping("/saveSkuInfo")
    public  void  saveSkuInfo(@RequestBody SkuInfo skuInfo){

        manageService.saveSkuInfo(skuInfo);
    }
}
