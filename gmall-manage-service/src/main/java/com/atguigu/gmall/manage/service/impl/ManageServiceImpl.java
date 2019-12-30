package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author GPX
 * @date 2019/12/25 18:32
 */

//dubbo服务端
@Service
public class ManageServiceImpl implements ManageService {


    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    SpuInfoMapper spuInfoMapper;

    @Resource
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SpuImageMapper spuImageMapper;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        List<BaseCatalog1> baseCatalog1List = baseCatalog1Mapper.selectAll();
        return baseCatalog1List;
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);

//        Example example = new Example(BaseCatalog2.class);
//        example.createCriteria().andEqualTo("catalog1Id",catalog1Id);
//
//        return baseCatalog2Mapper.selectByExample(example);

        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);
        return baseCatalog2List;
    }

    //同样实现 二级节点的查询 方法 更加 简洁
    @Override
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2) {

        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);

//        BaseCatalog3 baseCatalog31 = new BaseCatalog3();
//        baseCatalog31.setCatalog2Id(catalog2Id);
//        baseCatalog3Mapper.select(baseCatalog3);
        List<BaseCatalog3> baseCatalog3List = baseCatalog3Mapper.select(baseCatalog3);
        return baseCatalog3List;
    }

    /**
     * 三级节点数据展示
     *
     * @param catalog3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        //首页展示平台属性
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//
//        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.select(baseAttrInfo);
//        return baseAttrInfoList;

        //SKU添加平台属性回显 方法
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);


    }

    /**
     * 添加/修改
     *
     * @param baseAttrInfo
     */
    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //如果有主键就进行更新，如果没有就插入
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            baseAttrInfo.setId(null);
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //把原属性值全部清空 然后插入数据
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        //重新插入属性值
        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                //防止主键被赋上一个空字符串
                attrValue.setId(null);
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    /**
     * 修改商品属性值--不完整
     *
     * @param attrId
     * @return
     */
//    @Override
//    public List<BaseAttrValue> getAttrValueList(String attrId) {
//        BaseAttrValue baseAttrValue1 = new BaseAttrValue();
//        baseAttrValue1.setAttrId(attrId);
//        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue1);
//        return baseAttrValueList;
//    }

    /**
     * 修改商品属性值--功能完整
     * 回显商品属性
     *
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {

        //创建属性对象  先查属性
        BaseAttrInfo attrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // 创建属性值对象
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        // 根据attrId字段查询对象  从属性中查询属性值
        baseAttrValue.setAttrId(attrInfo.getId());
        //从属性中查询属性值
        List<BaseAttrValue> attrValueList = baseAttrValueMapper.select(baseAttrValue);
        // 给属性对象中的属性值集合赋值
        attrInfo.setAttrValueList(attrValueList);
        // 将属性对象返回
        return attrInfo;

    }

    /**
     * 获取SPU详情
     *
     * @param spuInfo
     * @return
     */
    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    /**
     * 回显销售属性
     *
     * @param
     */
    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    /**
     * 添加销售属性
     *
     * @param spuInfo
     */
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //四张表 分别添加
        //1.spuInfo 表
        spuInfo.setId(null);
        spuInfoMapper.insertSelective(spuInfo);

        //2.添加图片 spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setId(null);
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
        //3.spuSaleAttr 表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();

        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {

            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setId(null);
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //4 spuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();

                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {

                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setId(null);
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }

            }
        }

    }

    /**
     * 显示SKU图片信息
     *
     * @param spuImage
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    /**
     * SKU显示商品销售属性
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存Sku数据 四张表
     * sku_info，sku_attr_value，sku_sale_attr_value，sku_image
     *
     * @param skuInfo
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {

        //1.详情表 sku_info
        skuInfoMapper.insertSelective(skuInfo);

        //2.图片表 sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {
                if (skuImage.getId()!=null && skuImage.getId().length()==0){
                    skuImage.setId(null);
                }
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
        //3.销售属性表 sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                if (skuAttrValue.getId()!=null && skuAttrValue.getId().length()==0){
                    skuAttrValue.setId(null);
                }
                // skuId
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        //4.sku_sale_attr_value 销售属性值表
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                if (skuSaleAttrValue.getId()!=null && skuSaleAttrValue.getId().length()==0){
                    skuSaleAttrValue.setId(null);
                }
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }
}
