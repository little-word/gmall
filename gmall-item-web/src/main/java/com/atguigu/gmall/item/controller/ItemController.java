package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author GPX
 * @date 2019/12/30 14:28
 */
@Controller
@CrossOrigin
public class ItemController {
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;


    /**
     * 静态页面展示sku详情
     * @param skuId
     * @param request
     * @return
     */
    @LoginRequire
    @RequestMapping("/{skuId}.html")
    public String skuInfoPage(@PathVariable(value = "skuId") String skuId, HttpServletRequest request) {

        //页面图片展示
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);

        //SPU,SKU平台数据展示
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("saleAttrList", saleAttrList);

        //获取销售属性值Id  方式一
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        //把列表变换成 valueid1|valueid2|valueid3 ：skuId  的 哈希表 用于在页面中定位查询
        String valueIdsKey = "";

        Map<String, String> valuesSkuMap = new HashMap<>();

        //销售属性值切换
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            if (valueIdsKey.length() != 0) {
                valueIdsKey = valueIdsKey + "|";
            }
            //拼接格式 valueIds:125|127
            valueIdsKey = valueIdsKey + skuSaleAttrValue.getSaleAttrValueId();

            //判断数据库中有没有值  当前的SKUID与下一个SKUID不一致停止拼接
            if ((i + 1) == skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i + 1).getSkuId())) {

                valuesSkuMap.put(valueIdsKey, skuSaleAttrValue.getSkuId());
                valueIdsKey = "";
            }

        }

        //把map变成json串
        String valuesSkuJson = JSON.toJSONString(valuesSkuMap);
        System.out.println(valuesSkuJson);

        request.setAttribute("valuesSkuJson", valuesSkuJson);

        //点击其他销售属性值的组合，跳转到另外的sku页面 方式二
    //    GROUP_CONCAT：group_concat( [distinct] 要连接的字段 [order by 排序字段 asc/desc ] [separator '分隔符'] )
//        Map skuValueIdsMap = manageService.getSkuValueIdsMap(skuInfo.getSpuId());
//        request.setAttribute("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));


        //热点数据排序 详情页面
        listService.incrHotScore(skuId);
        return "item";
    }

    /**
     * 热点数据排序 详情页面
     * @param skuId
     * @return
     */
//    @RequestMapping("/{skuId}.html")
//    public String getSkuInfo(@PathVariable("skuId")String skuId){
//
//        //最终应该由异步方式调用
//        return
//    }
}
