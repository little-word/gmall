package com.atguigu.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    // 操作es的对象
    @Autowired
    private JestClient jestClient;


    @Test
    public void testES() throws IOException {
      /*
      1. 定义dsl 语句
            {
              "query": {
               "match": {
                 "actorList.name": "张译"
               }
              }
            }
      2. 定义执行的动作
         GET movie_chn/movie/_search
      3. 执行
      4. 获取结果
       */

        String query ="{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"actorList.name\": \"张国立\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // 在哪个index，type 中执行
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();
        // 执行search 查询动作
        SearchResult searchResult = jestClient.execute(search);

        // 获取数据
        List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);

        for (SearchResult.Hit<Map, Void> hit : hits) {
            Map map = hit.source;
            System.out.println(map);
        }
    }

}
