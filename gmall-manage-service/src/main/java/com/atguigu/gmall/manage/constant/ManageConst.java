package com.atguigu.gmall.manage.constant;

/**
 * @author GPX
 * @date 2019/12/30 20:50
 */
public class ManageConst {
    public static final String SKUKEY_PREFIX="sku:";

    public static final String SKUKEY_SUFFIX=":info";

    //数据的保存时间
    public static final int SKUKEY_TIMEOUT=7*24*60*60;

    //jedis 使用 redission 没有使用
    public static final int SKULOCK_EXPIRE_PX=10000;
    public static final String SKULOCK_SUFFIX=":lock";
}
