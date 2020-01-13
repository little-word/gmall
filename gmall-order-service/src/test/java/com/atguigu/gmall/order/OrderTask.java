package com.atguigu.gmall.order;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * 定时任务：测试
 * @author GPX
 * @date 2020/1/10 19:47
 */
@EnableScheduling
@Component
public class OrderTask {
    // 5 每分钟的第五秒
// 0/5 没隔五秒执行一次
    @Scheduled(cron = "5 * * * * ?")
    public void  work(){
        System.out.println("Thread ====== "+ Thread.currentThread());
    }
    @Scheduled(cron = "0/5 * * * * ?")
    public void  work1(){
        System.out.println("Thread1 ====== "+ Thread.currentThread());
    }
}
