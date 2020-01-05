package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GPX
 * @date 2020/1/3 12:54
 */
@Service
public class ListServiceImpl implements ListService {

    @Autowired
    //操作es的客户端
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    public static final String ES_INDEX = "gmall";
    public static final String ES_TYPE = "SkuInfo";

    /**
     * 存SkuInfo到es中
     *
     * @param skuLsInfo
     */
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {

        try {
            //建立索引的java语句
            Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

            DocumentResult documentResult = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 编写SDL 动态查询语句
     *
     * @param skuLsParams
     * @return
     */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {

        // 构造查询DSL
        String query = makeQueryStringForSearch(skuLsParams);

        //生成 动态查询的java语句  拼接成可执行的DSL语句

        //GET gmall/SkuInfo/_search{"query":{}.....}
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();

        SearchResult searchResult = null;
        try {
            //执行语句的到的结果
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //查询返回的结果集"hits": {
//        "total": 2,
//         "max_score": null,
//         "hits": []

        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);
        return skuLsResult;
    }

    /**
     * 构造查询DSL 处理查询 过滤 条件 语句的编写 在这里完成
     *
     * @param skuLsParams
     * @return
     */
    public String makeQueryStringForSearch(SkuLsParams skuLsParams) {

//        PUT gmall
//        "mappings": {
//            "SkuInfo":{
//                "properties": {
//                    "id":{"type": "keyword", "index": false},
//                    "price":{ "type": "double" },
//                     "skuName":{"type": "text", "analyzer": "ik_max_word" },
//                    "catalog3Id":{ "type": "keyword"},
//                    "skuDefaultImg":{"type": "keyword", "index": false},
//                    "skuAttrValueList":{"properties": {"valueId":{"type":"keyword"}


        //1. 创建查询的对象 执行对象build
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //过滤条件query :{
        //          bool :{filter:{ {"term": { "catalog3Id": "61"}},} //不分词查询 搜索精确度更高 一般用于Id 的具体的数值
        //              -->{must:{match:{ "skuName": "小米"}}} //分词查询
        //       "highlight": { "fields": {"skuName":{}} },
        //       "from": 1, "size": 2,


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 查询的过滤条件  must -macth 分词查询
        if (skuLsParams.getKeyword() != null) {
            MatchQueryBuilder match = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(match);

            //高亮 "highlight": { "fields": {"skuName":{}} },
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            //直接设置 设置字段
            highlightBuilder.field("skuName");
            //设置标签
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");

            // 将高亮结果放入查询器中
            searchSourceBuilder.highlight(highlightBuilder);
        }

        // 设置三级分类 term 精确查询
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder); //int 型
        }

        // 设置属性值 kuAttrValueList":{"properties": {"valueId":{"type":"keyword"}
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                TermQueryBuilder termsQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termsQueryBuilder);
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);


        //设置分页
        //       "from": 1, "size": 2,
        int from = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize(); //显示的数据下标
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //按照热度排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //聚合查询 分组查询
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();
        System.out.println("query=" + query);
        return query;
    }

    /**
     * 创建结果集
     *
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {

//        "hits": {
//            "total": 2,
//                    "max_score": null,
//                    "hits": [
//                "_index": "gmall",
//                    "_type": "SkuInfo",
//                    "_id": "3",
//                    "_score": null,
//                    "_source": {
//                        "id": "3",
//                        "price": 999,
//                        "skuName": "小米 红米5 Plus 全面屏拍照手机 全网通版 3GB+32GB 金色 移动联通电信4G手机 双卡双待",
//                        "catalog3Id": "61",
//                        "skuDefaultImg": "http://file.service.com/group1/M00/00/00/wKhDyVrvp0uAXEdMAABvI_LYeVc795.jpg",
//                        "hotScore": 0,
//                        "skuAttrValueList": [],highlight": { "skuName": [  ]}"sort": [0 ]

        //处理 命中结果 高亮显示  分组统计结果
        SkuLsResult skuLsResult = new SkuLsResult();

        //集合的初始容量 达到最大可以 自动扩容
        ArrayList<SkuLsInfo> skuLsInfoList = new ArrayList<>(skuLsParams.getPageSize());

        //获取sku列表
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source; //命中的原始数据
            if (hit.highlight != null && hit.highlight.size() > 0) {
                //获取高亮字段
                List<String> list = hit.highlight.get("skuName");
                //把带有高亮标签的字符串替换skuName
                String skuNameHigh = list.get(0);
                skuLsInfo.setSkuName(skuNameHigh);
            }
            skuLsInfoList.add(skuLsInfo);
        }

        //返回结果中 添加 属性 完成返回的结果结合 用于展示页面的返回结果
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        skuLsResult.setTotal(searchResult.getTotal());

        //取记录个数并计算出总页数
        long totalPage = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

        //取出涉及的属性值id
        ArrayList<String> attrValueIdList = new ArrayList<>();
        //聚合查询  分组查询
//        aggregations": {
//        "groupby_attr": {
//            "doc_count_error_upper_bound": 0,
//                    "sum_other_doc_count": 0,
//                    "buckets": [
//                "key": "13",
//                    "doc_count": 2
//                "key": "16",
//                    "doc_count": 2
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        if (groupby_attr != null) {
            List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                attrValueIdList.add(bucket.getKey());
            }
            //List<SkuAttrValue> skuAttrValueList; 与skuInfo 的属性一致 添加时作为key 来保存数据
//         "hotScore": 0,  skuAttrValueList": [
//            {
//                "id": "3",
//                    "attrId": "23",
//                    "valueId": "13",
//                    "skuId": "3"
            skuLsResult.setAttrValueIdList(attrValueIdList);
        }
        //es 返回的条件查询的最终 组合结果
        return skuLsResult;
    }
    //最终结果 用到的部分展示
//        "hits": {
//          "total": 2,
//          "max_score": null,
//          "hits": [  {
//            "_index": "gmall",
//                "_type": "SkuInfo",
//                "_id": "3",
//                "_score": null,
//                "_source": {//查询到的元数据
//                     "id": "3",
//                    "price": 999,
//                    "skuName": "小米 红米5 Plus 全面屏拍照手机 全网通版 3GB+32GB 金色 移动联通电信4G手机 双卡双待",
//                    "catalog3Id": "61",
//                    "skuDefaultImg": "http://file.service.com/group1/M00/00/00/wKhDyVrvp0uAXEdMAABvI_LYeVc795.jpg",
//                    "hotScore": 0,
//              ***** "skuAttrValueList": [ {
//                     "id": "4",  "attrId": "24", "valueId": "16", "skuId": "3" }  ]   },
//            "highlight": { "skuName": [ "<em>小米</em> 红米5 Plus 全面屏拍照手机 全网通版 3GB+32GB 金色 移动联通电信4G手机 双卡双待" ] },
//            "sort": [ 0]} ] },
//            "aggregations": {
//              "groupby_attr": {"doc_count_error_upper_bound": 0,"sum_other_doc_count": 0,   "buckets": [{"key": "13","doc_count": 2 }, { "key": "16", "doc_count": 2 }  ] }} }


    /**
     * 热点数据排序 hotScore 在 skuLsinfo 类中 控制器 在ItemController中
     * 更新热度评分 利用redis做精确计数器
     * 利用redis的原子性的自增可以解决并发写操作
     *
     * @param skuId
     * @return
     */
    @Override
    public String incrHotScore(String skuId) {

        Jedis jedis = redisUtil.getJedis();
        // redis每计10次数 执行一次es timeTotalEs
        int timesToEs=10;
        //每次自增一 zset数据类型
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId" + skuId);
        if (hotScore % timesToEs == 0){
            //Math.round 取向上的整数
            updateHotScore(skuId,Math.round(hotScore));
        }
        return null;
    }

    //更新es
    private void updateHotScore(String skuId, long hotScore) {
        String updateJson="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" +
                "}";
        Update update = new Update.Builder(updateJson).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
