package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author GPX
 * @date 2019/12/25 18:32
 */

//dubbo服务端
@Service
public class ManageServiceImpl implements ManageService {


    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Resource
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

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
                if (skuImage.getId() != null && skuImage.getId().length() == 0) {
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
                if (skuAttrValue.getId() != null && skuAttrValue.getId().length() == 0) {
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
                if (skuSaleAttrValue.getId() != null && skuSaleAttrValue.getId().length() == 0) {
                    skuSaleAttrValue.setId(null);
                }
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    /**
     * 热点数据查询 使用到redis缓存机制 ---》涉及到redis的缓存穿透--缓存击穿
     * 静态页面展示sku详情
     * 使用redis
     * 解决缓存击穿
     *
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {

//        SkuInfo skuInfo = null;
//        try{
//            Jedis jedis = redisUtil.getJedis();
//            // 定义key  拼接key-->sku:43:info
//            String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX; //key= sku:skuId:info
//              获取json值
//            String skuJson = jedis.get(skuInfoKey);
//
//            if (skuJson==null || skuJson.length()==0){
//                // 没有数据 ,需要加锁！取出完数据，还要放入缓存中，下次直接从缓存中取得即可！
//                System.out.println("没有命中缓存");  //走数据库--getSkuInfoDB
//                // 定义穿透锁 key  key---> user:userId:lock
//                String skuLockKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
//             String token = UUID.randomUUID().toString().substring(4, 6).replaceAll("-", "");
//                // 生成锁  ManageConst.SKULOCK_SUFFIX--睡眠时间
//                String lockKey  = jedis.set(skuLockKey, token, "NX", "PX",ManageConst.SKULOCK_SUFFIX);//添加完reids返回的结果就是OK
//                if ("OK".equals(lockKey)){
//                    System.out.println("获取锁！");
//                    // 从数据库中取得数据
//                    skuInfo = getSkuInfoDB(skuId);
//                    // 将是数据放入缓存
//                    // 将对象转换成字符串
//                    String skuRedisStr = JSON.toJSONString(skuInfo);
//                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);
        //删除锁
//                   String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//                   jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(token));
//                    jedis.close();
//                    return skuInfo;
//                }else {
//                      没获取到锁
//                    System.out.println("等待！");
//                    // 等待
//                    Thread.sleep(1000);
//                    // 自旋 时间一到 调用自己 获取锁
//                    return getSkuInfo(skuId);
//                }
//            }else{
//                // 查询到缓存
//                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
//                jedis.close();
//                return skuInfo;
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        // 从数据库返回数据
//        return getSkuInfoDB(skuId);
//    }
        return getSkuInfoRedisson(skuId);
    }

    /**
     * 使用redisson解决分布式锁
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(String skuId) {
        // 业务代码
        SkuInfo skuInfo = null;
        RLock lock = null;
        Jedis jedis = null;
        try {

            jedis = redisUtil.getJedis();
            // 定义key  sku:43:info
            String userKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            if (jedis.exists(userKey)) {
                // 获取缓存中的数据
                String userJson = jedis.get(userKey);
                //获取到数据
                if (!StringUtils.isEmpty(userJson)) {
                    skuInfo = JSON.parseObject(userJson, SkuInfo.class);
                    return skuInfo;
                }
            } else {
                // 创建
                //
                // 这里使用redission
                Config config = new Config();
                // redis://192.168.126.129:6379 配置文件中！  没有集群 使用useSingleServer
                config.useSingleServer().setAddress("redis://192.168.126.129:6379");

                RedissonClient redisson = Redisson.create(config);

                //my-lock 自定义 可以改变
                lock = redisson.getLock("my-lock");

                //上锁后自动解锁时间10s   最低10s 最长等待时间100s
                lock.lock(10, TimeUnit.SECONDS);

                // 从数据库查询数据
                skuInfo = getSkuInfoDB(skuId);
                // 将数据放入缓存
                // jedis.set(userKey,JSON.toJSONString(skuInfo));
                jedis.setex(userKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (lock != null) {
                lock.unlock();
            }

        }
        // 从数据库查询！
        return getSkuInfoDB(skuId);
    }

    /**
     * 从数据库中获取数据
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(String skuId) {
        //商品详情信息
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //获取sku的图片
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        //将图片添加到skuInfo 用来前端页面展示
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);

        // 查询属性值
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        // 将查询出来所有商品属性值赋给对象
        skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueList);

        //获取skuAttrValue 平台属性用于 es建立索引
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);
        return skuInfo;


        // select * from skuInfo where id = skuId;
        // 通过skuId 将skuImageList 查询出来直接放入skuInfo 对象中！
//        SkuInfo skuInfo;
//        skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
//        skuInfo.setSkuImageList(getSkuImageBySkuId(skuId));
//        return   skuInfo;
    }


    /**
     * 前端展示 平台销售属性
     *
     * @param skuInfo
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());

    }

    /**
     * 点击其他销售属性值的组合，跳转到另外的sku页面 方式二
     * @param spuId
     * @return
     */
//    @Override
//    public Map getSkuValueIdsMap(String spuId) {
//
//        List<Map> mapList=skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
//        HashMap<Object, Object> hashMap = new HashMap<>();
//        for (Map map : mapList) {
//            hashMap.put(map.get("value_ids"),map.get("sku_id"));
//        }
//        return hashMap;
//    }

    /**
     * 点击其他销售属性值的组合，跳转到另外的sku页面 方式一
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
        return skuSaleAttrValueList;
    }

    /**
     * DSL  全局查询 返回的属性值集合
     *
     * @param attrValueIdList
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {

        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        return baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);
    }
}
