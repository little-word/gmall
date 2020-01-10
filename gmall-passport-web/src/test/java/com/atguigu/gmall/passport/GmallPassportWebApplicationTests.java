package com.atguigu.gmall.passport;

import com.atguigu.gmall.passport.util.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {


    @Test
    public void test01() {
        String key = "atguigu";
        String ip = "192.168.126.129";
        Map map = new HashMap();
        map.put("userId", "1001");
        map.put("nickName", "marry");
        String token = JwtUtil.encode(key, map, ip);
        Map<String, Object> decode = JwtUtil.decode(token, key, "192.168.126.129");
        System.out.println("token:"+token);
    }
}
