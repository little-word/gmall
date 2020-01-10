package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

/**
 * @author GPX
 * @date 2020/1/7 13:08
 */
@Service
public class CartServiceImpl implements CartService {

    public static final String USER_KEY_PREFIX = "user:";

    public static final String USER_CART_KEY_SUFFIX = ":cart";

    public static final String USER_CHECKED_KEY_SUFFIX = ":checked";

    public static final String USERINFOKEY_SUFFIX = ":info";

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Reference
    private ManageService manageService;


    /**
     * 添加到购物车
     * 1.  先查看数据库中该商品是否存在
     * select * from cartInfo where userId = ? and skuId = ?
     * true: 数量相加upd
     * false: 直接添加
     * 2.  放入redis！
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {

        //将数据保存到redis中
        Jedis jedis = redisUtil.getJedis();

        //定义key user:userId:cart 需要放入缓存一份
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        //测试优化在添加购物车的时候，需要判断缓存中是否有key，
        // 如果没有key则查询数据库，并添加到缓存。避免数据丢失！
        // 调用查询数据库并加入缓存 存在走缓存 不存在走db
        if (!jedis.exists(cartKey)) {
            loadCartCache(userId);//走db
        }

        //查看数据库 添加到数据库
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId", userId).andEqualTo("skuId", skuId);
        //获取数据库中cartInfo
        List<CartInfo> cartInfoList = cartInfoMapper.selectByExample(example);

        CartInfo cartInfoExist = null;
        if (cartInfoList != null && cartInfoList.size() > 0) {
            cartInfoExist = cartInfoList.get(0);//第一条数据 每条数据都是同一个人的同一个userid
        }
        //获取到数据 证明数据库中已存在
        if (cartInfoExist != null) {
            //数据库中的数量 加上 用户 从页面选中的 数量  更新数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            // 初始化实时价格 单件商品
            cartInfoExist.setCartPrice(cartInfoExist.getCartPrice());

            //更新数据库库存
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        } else {
            //初次购买 将商品 详情加入到 cartInfo 表中 购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);//商品的详情

            //购物车对象
            CartInfo cartInfo = new CartInfo();

            //添加属性
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());

            //添加到数据库 未登录时 保存临时id
            cartInfoMapper.insert(cartInfo);

            //为了避免代码冗余 少创建一个对象 用于更新redis！  CartInfo cartInfoExist = null;
            cartInfoExist = cartInfo;
        }
        //对象转化为json保存到redis
        String cartInfoJson = JSON.toJSONString(cartInfoExist);

        // 更新redis 放在最后！
        jedis.hset(cartKey, skuId, cartInfoJson);
        //设置key的存在时间
        setCartkeyExpireTime(userId, jedis, cartKey);

        //不要忘记关闭
        jedis.close();
    }

    /**
     * 从数据库获得购物车详情 并加载到redis中
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> loadCartCache(String userId) {

        // 使用实时价格：将skuInfo.price 价格赋值 cartInfo.skuPrice  购买数量的集合
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoList == null || cartInfoList.size() == 0) {
            //db没有有数据
            return null;
        }

        // 有数据----- 数据库有 redis没有

        //1.获取jedis
        Jedis jedis = redisUtil.getJedis();

        //定义临时购物车的key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        //存放 每个购买商品的详情
        Map<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
        }
        //存放到jedis中
        jedis.hmset(cartKey, map);

        //重新设置时间
        setCartkeyExpireTime(userId, jedis, cartKey);
        jedis.close();
        return cartInfoList;

    }

    /**
     * 设置cartInfo 在redis中的 保存时间 过期时间
     *
     * @param userId
     * @param jedis
     * @param cartKey
     */
    private void setCartkeyExpireTime(String userId, Jedis jedis, String cartKey) {
        // 获取用户的过期时间 并设置时间

        //根据userId 拼接 保存用户信息的key user:userId:info
        // 用户的登录信息key 在单点登录保存的
        String userKey = CartConst.USER_KEY_PREFIX + userId + USERINFOKEY_SUFFIX;

        Long expireTime = null;
        if (jedis.exists(userKey)) {
            //获取过期时间 剩余时间---用户登录信息
            expireTime = jedis.ttl(userKey);
            // 给购物车的 key设置时间  如果登录了
            // 将用户的过期时间 设置给购物车的过期时间  保证同时过期
            jedis.expire(cartKey, expireTime.intValue());
        } else {
            //如果不存在key  未登录使用临时id 将数据保留7天 addCart时添加的临时id
            jedis.expire(cartKey, 7 * 24 * 3600);
        }
    }


    /**
     * 根据userId 查询购物车列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId) {

        //放在集合中传递
        List<CartInfo> cartInfoList = new ArrayList<>();
        //获取redis中的购物车数据 没有从数据库中查
        Jedis jedis = redisUtil.getJedis();

        // 定义key：user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        //获取key的值 商品的数量 从缓存中获取的
//        jedis.hgetAll()== `hvals() 缓存有值
        List<String> stringList = jedis.hvals(cartKey);
        if (stringList != null && stringList.size() > 0) {
            for (String cartJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);

                //存入集合 用于页面展示
                cartInfoList.add(cartInfo);
            }
            //给商品排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        } else {
            /// 走db -- 放入redis  加载缓存
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    /**
     * 合并购物车 合并未登录的购物车数据
     * List<CartInfo> cartInfoArrayList 未登录的数据
     *
     * @param cartInfoArrayList
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoArrayList, String userId) {
        // 获取到登录时购物车数据
        List<CartInfo> cartInfoListLogin = cartInfoMapper.selectCartListWithCurPrice(userId);
        // 判断登录时购物车数据是否为空
        if (cartInfoListLogin != null && cartInfoListLogin.size() > 0) {
            //未登录 cartInfoArrayList 临时id获取的
            for (CartInfo cartInfoNoLogin : cartInfoArrayList) {
                // 声明一个boolean 类型变量 isMatch是否匹配 登录是购物车的商品 与未登录时购物车的商品 标识
                boolean isMatch = false;
                for (CartInfo cartInfoLogin : cartInfoListLogin) {
                    //判断 是否匹配
                    if (cartInfoNoLogin.getSkuId().equals(cartInfoLogin.getSkuId())) {
                        //匹配到 有相同商品 数量加一
                        cartInfoLogin.setSkuNum(cartInfoNoLogin.getSkuNum() + cartInfoLogin.getSkuNum());
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoLogin);
                        System.out.println("mergeToCartList:合并购物车 数据库有数据 合并成功"+cartInfoLogin);
                        //匹配到了
                        isMatch = true;
                    }
                }
                // 表示登录的购物车数据与未登录购物车数据  没有匹配上！
                if (!isMatch) {
                    cartInfoNoLogin.setId(null);
                    cartInfoNoLogin.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoNoLogin);
                    System.out.println("mergeToCartList:合并购物车 购物车有商品存在 但没有这件商品 添加成功"+cartInfoNoLogin);
                }
            }
        } else {
            //数据库为空 直接添加数据  合并到到登录状态下的购物车
            for (CartInfo cartInfo : cartInfoArrayList) {
                cartInfo.setId(null);
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
                System.out.println("数据库购物车没有任何数据 直接添加到购物车");
            }
        }
        //合并缓存后的数据 重新查询数据库
        List<CartInfo> cartInfoList = loadCartCache(userId);

        //合并 选中的状态
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfo : cartInfoArrayList) {
                //skuId 相同同一件商品
                if (cartInfoDB.getSkuId().equals(cartInfo.getSkuId())) {
                    // 如果数据库中为1，未登录中也为1 不用修改！
                    if ("1".equals(cartInfo.getIsChecked())) {
                        if ("1".equals(cartInfoDB.getIsChecked())) {
                            // 修改数据库字段为1
                            cartInfoDB.setIsChecked("1");
                            // 修改商品状态为被选中
                            checkCart(cartInfo.getIsChecked(), cartInfo.getSkuId(), userId);
                        }
                    }
                }
            }
        }
        return cartInfoList;
    }

    /**
     * 删除数据库中 redis 中
     * 临时id存储的数据
     * @param userTempId
     */
    @Override
    public void deleteCartList(String userTempId) {

        // 先删除表中的数据
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId", userTempId);
        cartInfoMapper.deleteByExample(example);

        //删除redis缓存
        Jedis jedis = redisUtil.getJedis();
        //cartKey=user:ddbed2fc03a143368fc8709e51c65ceb:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userTempId + CartConst.USER_CART_KEY_SUFFIX;
        jedis.del(cartKey);

        jedis.close();
    }

    /**
     * 修改购物车中 商品的状态 选中/未选中
     * 去结算使用
     * @param isChecked
     * @param skuId
     * @param userId
     */
    @Override
    public void checkCart(String isChecked, String skuId, String userId) {

        //实现1.修改缓存 2.修改数据库
        // 第一种方案：直接修改缓存
        Jedis jedis = redisUtil.getJedis();
        //  key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

//        //获取 key值商品数据 hsah类型
//        String cartJson  = jedis.hget(cartKey, skuId);
//        CartInfo cartInfoJson = JSON.parseObject(cartJson,CartInfo.class);
//        //修改数据 将修改之后的数据放入缓存
//        cartInfoJson.setIsChecked(isChecked);
//        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoJson));

        // 修改数据库中数据 update cartInfo set is_checked = ? where userId = ? and skuId = ?
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId", userId).andEqualTo("skuId", skuId);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        System.out.println("checkCart:isChecked数据库修改数据成功");
        cartInfoMapper.updateByExampleSelective(cartInfo, example);

        // 第二种：按照缓存管理的原则：避免出现脏数据，先删除缓存，再放入缓存
        //1.删除缓存
        jedis.hdel(cartKey, skuId);
        //2.放入缓存
        List<CartInfo> cartInfoList = cartInfoMapper.selectByExample(example);
        // 获取集合数据第一条数据，（因为一次只能点一个 选中一件商品 不能点一个 选多个）
        if (cartInfoList != null && cartInfoList.size() > 0) {
            CartInfo cartInfoQuery = cartInfoList.get(0);
            // 数据初始化实时价格！
            cartInfoQuery.setSkuPrice(cartInfoQuery.getCartPrice());
            //放入缓存
            jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfoQuery));
        }
        jedis.close();
    }

    /**
     * 获取所有选中物品的详情
     *tradeInit 在订单方法
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        //先从缓存 中查询
        Jedis jedis = redisUtil.getJedis();

        //放入缓存的选中商品 keyuser:1:isChecked
        String ckeckedKey = CartConst.USER_KEY_PREFIX + userId + ":isChecked";
        jedis.del(ckeckedKey);

        //购物车key user:1:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        //保存  获取到的购物车选中的每件商品   将选中的商品放入缓存 放入前删除 user:1:isChecked
        List<CartInfo> cartInfoList = new ArrayList<>();
        //从缓存中获取 添加购物车时 合并并加入了redis中 在checkCart方法中
        List<String> cartList = jedis.hvals(cartKey);

        if (cartList != null && cartList.size() > 0) {
            for (String cartJson : cartList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                if ("1".equals(cartInfo.getIsChecked())) {
                    cartInfoList.add(cartInfo);
                }
            }
        }

        //选中的商品 key；user:1:isChecked
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId);
        List<CartInfo> ckeckedInfo = cartInfoMapper.selectByExample(example);
        ArrayList<CartInfo> ckeckedCartInfo = new ArrayList<>();

        for (CartInfo cartCkecked : ckeckedInfo) {
            if ("1".equals(cartCkecked.getIsChecked())){
                ckeckedCartInfo.add(cartCkecked);
            }
        }
        //选中商品放入redis
        jedis.setex(ckeckedKey,3*24*3600, JSON.toJSONString(ckeckedCartInfo));

        jedis.close();
        return cartInfoList;
    }
}
