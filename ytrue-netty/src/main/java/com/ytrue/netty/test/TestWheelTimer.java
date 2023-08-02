package com.ytrue.netty.test;

import com.ytrue.netty.util.HashedWheelTimer;
import com.ytrue.netty.util.Timeout;
import com.ytrue.netty.util.TimerTask;
import com.ytrue.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-02 13:53
 * @description TestWheelTimer
 */
public class TestWheelTimer {

    public static void main(String[] args) {



        HashedWheelTimer timer = new HashedWheelTimer(new DefaultThreadFactory("时间轮",false,5), 1, TimeUnit.SECONDS,8);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("1秒执行1次");
            }
        };


        timer.newTimeout(timerTask, 1, TimeUnit.SECONDS);


    }
}
