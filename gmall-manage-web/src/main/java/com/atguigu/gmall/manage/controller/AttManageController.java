package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.web.bind.annotation.*;


/**
 * @author GPX
 * @date 2020/1/3 12:56
 */
@RestController
@CrossOrigin
public class AttManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    /**
     * 保存数据到es中
     * 上架
     * @param skuId
     */
    @RequestMapping(value = "/onSale",method = RequestMethod.GET)
    @ResponseBody
    public void skuLsInfo(String skuId) {

        SkuLsInfo skuLsInfo = null;
        try {
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            skuLsInfo = new SkuLsInfo();
            BeanUtils.copyProperties(skuInfo, skuLsInfo);
        } catch (BeansException e) {
            e.printStackTrace();
        }

        listService.saveSkuInfo(skuLsInfo);
    }
}
