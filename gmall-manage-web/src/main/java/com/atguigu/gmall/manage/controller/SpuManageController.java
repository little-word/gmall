package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * @author GPX
 * @date 2019/12/27 17:16
 */
@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    /**
     * 获取商品SPU
     * http://localhost:8082/spuList?catalog3Id=1
     * <p>
     * public List<SpuInfo> spuList(String catalog3Id)
     * SpuInfo spuInfo = new SpuInfo();
     * spuInfo.setCatalog3Id(catalog3Id);
     *
     * @param spuInfo
     * @return
     */
    @RequestMapping("/spuList")
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return manageService.getSpuInfoList(spuInfo);
    }

    /**
     * 加载销售属性名称列表  添加/修改
     * http://localhost:8082/baseSaleAttrList
     */
    @RequestMapping("/baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList() {

        return manageService.baseSaleAttrList();

    }

    /**
     * 保存销售属性，图片属性，
     * spuInfo
     * spuImage
     * spuSaleAttr
     * spuSaleAttrValue
     * http://localhost:8082/saveSpuInfo
     */
    @RequestMapping("/saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo) {

        manageService.saveSpuInfo(spuInfo);
    }


}
