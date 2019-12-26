package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author GPX
 * @date 2019/12/26 11:16
 */
@RestController
@CrossOrigin
public class ManageController {

    //dubbo消费端
    @Reference
    private ManageService manageService;

    /**
     * 获取所有一级分类节点
     *  List<BaseCatalog1> getCatalog1();
     * @return
     */
    @RequestMapping("/getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return  manageService.getCatalog1();
    }
    /**
     * List<BaseCatalog2> getCatalog2(String catalog1Id);
     * 根据一级节点Id获取所有二级分类节点
     */
    @RequestMapping("/getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){

      return   manageService.getCatalog2(catalog1Id);
    }

    /**
     * List<BaseCatalog3> getCatalog3(String catalog2Id);
     * 根据二级节点Id获取所有三级分类节点
     */
    @RequestMapping("/getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return  manageService.getCatalog3(catalog2Id);
    }

    /**
     *  List<BaseAttrInfo> getAttrList(String catalog3Id);
     * 根据三级节点Id获取获取平台属性
     */
    @RequestMapping("/attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }

    /**
     *
     * http://localhost:8082/saveAttrInfo
     * 添加三级分类的属性
     */

    @RequestMapping("/saveAttrInfo")
    public void  saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
    }
//    /**
//     * 修改商品属性值（课件）
//     */
//    @RequestMapping(value = "/getAttrValueList",method = RequestMethod.POST)
//    public List<BaseAttrValue> getAttrValueList(String attrId){
//        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
//
//       // return  manageService.getAttrValueList(attrId);
//        return attrInfo.getAttrValueList();
//    }
    /**
     * 修改商品属性值
     */
    @RequestMapping(value = "/getAttrValueList",method = RequestMethod.POST)
    public List<BaseAttrValue> getAttrValueList(String attrId){

        return manageService.getAttrValueList(attrId);
    }

}
