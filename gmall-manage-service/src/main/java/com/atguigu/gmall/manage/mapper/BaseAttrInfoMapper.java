package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    //SKU添加平台属性回显
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);
}
