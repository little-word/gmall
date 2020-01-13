package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;

import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.gware.util.HttpclientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
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

    @Autowired
    private ActiveMQUtil activeMQUtil;


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

//     删除key   jedis.del(tradeNoKey);

        // jedis.del(tradeNoKey); lua脚本
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
        //校验库存 重定向到 库存项目 远程调用 库存管理系统的接口
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

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);

        //获取订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    /**
     * 收到消息后 更新订单状态
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    /**
     * 发送消息队列
     * @param orderId
     */
    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        //初始化库存
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);

            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);
            producer.send(textMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


    /**
     * 初始化库存量
     * @param orderId
     * @return
     */
    public String initWareOrder(String orderId) {

        //将orderinfo 中需要的信息保存到map返回map
        OrderInfo orderInfo = getOrderInfo(orderId);
        //初始化支付后订单的方法
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 1 先查询原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // 2 wareSkuMap 反序列化
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        // 3 遍历拆单方案
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 4 生成订单主表，从原始订单复制，新的订单号，父订单
            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(subOrderInfo,orderInfoOrigin);
            subOrderInfo.setId(null);
            // 5 原来主订单，订单主表中的订单状态标志为拆单 分库
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            subOrderInfo.setWareId(wareId);

            // 6 明细表 根据拆单方案中的skuids进行匹配，得到那个的子订单
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            // 创建一个新的订单集合
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    //这里进行拆分
                    if (skuId.equals(orderDetail.getSkuId())){
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }
            //拆分的不同订单
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            subOrderInfo.sumTotalAmount();
            // 7 保存到数据库中
            saveOrder(subOrderInfo);
            subOrderInfoList.add(subOrderInfo);
        }
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        // 8 返回一个新生成的子订单列表
        return subOrderInfoList;
    }

    // 设置初始化仓库信息方法
    public Map initWareOrder(OrderInfo orderInfo) {

        //给map赋值
        Map<String,Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        //获取发送订单的信息
        map.put("orderBody","测试库存减少");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");

        //仓库id拆单使用
        map.put("wareId",orderInfo.getWareId());

        // 组合json OrderDetail
        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map detailMap = new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());

            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }

    /**
     * 处理过期订单
     * @param orderInfo
     */
    @Override
    public void execExpiredOrder(OrderInfo orderInfo) {
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
    }

    @Override
    public OrderInfo getOrderInfo(OrderInfo orderInfo) {
        return orderInfoMapper.selectOne(orderInfo);
    }
}
