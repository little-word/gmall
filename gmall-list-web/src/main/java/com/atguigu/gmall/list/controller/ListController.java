package com.atguigu.gmall.list.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author GPX
 * @date 2020/1/3 21:25
 */
@Controller
@CrossOrigin
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;


    /**
     * 返回查询到的数据详情
     * 在页面展示
     * 返回属性和属性值列表
     *
     * @param skuLsParams
     * @return
     */
    @RequestMapping("/list.html")
//    @ResponseBody  //HttpServletRequest request /Model model
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request) {

        //返回的DSL语句 查询的结果
        SkuLsResult skuLsResult = listService.search(skuLsParams);


        //获取SKU属性列表
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        // 从结果中取出平台属性值列表  List<String> attrValueIdList; 只有ValueID
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //取得具体的属性值 用于<h3><em>商品筛选</em></h3>
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        //面包屑 存放的集合
        List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();

        //http://list.gmall.com/list.html?keyword=手机&catalog3Id=61&valueId=13
        String urlParam = makeUrlParam(skuLsParams);
        //移除baseAttrInfoList 中的valueId 和skuLsInfoList 中valueId 相同的 valueId
        // 选择筛选条件后 将筛选条件删除
        // 让用户有更好的体验
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            //平台属性
            BaseAttrInfo baseAttrInfo = iterator.next();
            //获取平台属性集合对象
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {

                    for (String valueId : skuLsParams.getValueId()) {
                        //选中的属性值 和 查询结果的属性值
                        if (valueId.equals(baseAttrValue.getId())) {
                            iterator.remove();
                            //获取面包屑 移除的平台属性值

                            BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                            //        "数据" 运行内存:3G ✖ 机身内存:64G ✖  keyword(skuName): baseAttrName:baseAttrValue
                            baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());

                            // 去除重复数据
                            String makeUrlParam = makeUrlParam(skuLsParams, valueId);
                            baseAttrValueSelected.setUrlParam(makeUrlParam);
                            baseAttrValuesList.add(baseAttrValueSelected);

                        }
                    }
                }
            }
        }

        //分页
        skuLsParams.setPageSize(2);
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        //保存面包屑
        request.setAttribute("baseAttrValuesList", baseAttrValuesList);
        //"数据"(keyword) 运行内存:3G
        request.setAttribute("keyword", skuLsParams.getKeyword());
        //返回拼接地址 过滤请求地址
        request.setAttribute("urlParam", urlParam);
        // 返回平台属性
        request.setAttribute("baseAttrInfoList", baseAttrInfoList);
        //返回商品详情
        request.setAttribute("skuLsInfoList", skuLsInfoList);
//        return JSON.toJSONString(search);
        return "list";
    }

    /**
     * 携带属性值 进行过滤 查询
     *
     * @param skuLsParams
     * @return
     */
    public String makeUrlParam(SkuLsParams skuLsParams, String... excludeValueIds) {

        //拼接查询的地址
        String urlParam = "";
        if (skuLsParams.getKeyword() != null) {

            //http://list.gmall.com/list.html?keyword=手机&catalog3Id=61&valueId=13
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }
        //判断三级分类Id 拼接catalog3Id
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            if (urlParam.length() > 0) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }

        //拼接valueId平台属性值Id  面包屑功能
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (String valueId : skuLsParams.getValueId()) {
                //每次点击平台属性展示对应的面包屑  面包屑功能 展示
                if (excludeValueIds != null && excludeValueIds.length > 0) {
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)) {
                        // 跳出代码，后面的参数则不会继续追加【后续代码不会执行】
                        // 不能写break；如果写了break；其他条件则无法拼接！
                        continue;
                    }
                }
                if (urlParam.length() > 0) {
                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;
            }
        }
        return urlParam;
    }
    //热点数据排序业务在  itemController
}