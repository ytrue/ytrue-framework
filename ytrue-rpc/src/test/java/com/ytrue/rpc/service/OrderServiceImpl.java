package com.ytrue.rpc.service;

/**
 * @author ytrue
 * @date 2023-05-20 13:09
 * @description OrderServiceImpl
 */
public class OrderServiceImpl implements OrderService {
    @Override
    public void test01() {
        System.out.println("test01..........");
    }

    @Override
    public String test01(String str) {
        System.out.println("test02...................");
        return str;
    }
}
