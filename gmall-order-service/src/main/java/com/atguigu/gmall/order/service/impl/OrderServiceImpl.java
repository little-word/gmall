package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;

import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.gware.util.HttpclientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * @author GPX
 * @date 2020/1/8 19:42
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;


    /**
     * 保存订单
     * 用户的订单数据 包含每条订单的数据 ，订单的状态等详细信息 --> 送货清单
     *
     * @param orderInfo
     * @return
     */
    @Override
    public String saveOrder(OrderInfo orderInfo) {

        //创建时间，过期时间
        orderInfo.setCreateTime(new Date());

        //过期时间1个月
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        //第三方支付编号 0- 1000产生一个随机数 获取时间戳System.currentTimeMillis()
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);

        // 插入订单详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            //每一件商品唯一的 id
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 为了跳转到支付页面使用。支付会根据订单id进行支付
        String orderId  = orderInfo.getId();
        return orderId ;
    }

    /**
     * 生成流水号 解决用户利用浏览器回退重复提交订单
     *  在进入结算页面时，生成一个结算流水号，然后保存到结算页面的隐藏元素中，
     *  每次用户提交都检查该流水号与页面提交的是否相符，订单保存以后把后台的流水号删除掉。
     *  那么第二次用户用同一个页面提交的话流水号就会匹配失败，无法重复保存订单。
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    /**
     * 验证流水号
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:"+userId+":tradeCode";
        jedis.close();
        //判断是否能获取到
        String tradeCode  = jedis.get(tradeNoKey);
        if (tradeCode != null && tradeCode.equals(tradeCodeNo)){
            //订单已提交 不能重复提交
            return true;
        }else {
            //提交订单
            return false;
        }
    }

    /**
     * 删除流水号
     * @param userId
     */
    @Override
    public void delTradeNo(String userId) {
        // 获取jedis 中的流水号
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey ="user:"+userId+":tradeCode";

        String tradeCode = jedis.get(tradeNoKey);


        // jedis.del(tradeNoKey);
        String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedis.eval(script, Collections.singletonList(tradeNoKey),Collections.singletonList(tradeCode));

        jedis.close();

    }

    /**
     *
     * 校验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        //校验库存 从定向到 库存项目
        String result = HttpclientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            //有库存
            return  true;
        }else {
            //没有库存
            return  false;
        }

    }

    /**
     * 获取订单详情
     * 获取订单总金额 用于支付
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {

        return orderInfoMapper.selectByPrimaryKey(orderId);
    }
}
