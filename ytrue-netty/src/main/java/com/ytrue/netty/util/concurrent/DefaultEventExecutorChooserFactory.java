package com.ytrue.netty.util.concurrent;

import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-07-24 9:27
 * @description DefaultEventExecutorChooserFactory
 */

public class DefaultEventExecutorChooserFactory implements EventExecutorChooserFactory {

    private DefaultEventExecutorChooserFactory() {
    }

    public static final DefaultEventExecutorChooserFactory INSTANCE = new DefaultEventExecutorChooserFactory();

    @Override
    public EventExecutorChooser newChooser(EventExecutor[] executors) {
        //如果数组的长度是2的幂次方就返回PowerOfTwoEventExecutorChooser选择器
        if (isPowerOfTwo(executors.length)) {
            return new PowerOfTwoEventExecutorChooser(executors);
        } else {
            //如果数的
            //思想呢？计算数据下标的时候。&会比直接%效率要快一点。这就是netty中微不足道的一个小小优化。组长度不是2的幂次方就返回通用选择器。其实看到2的幂次方，应该就可以想到作者考虑的是位运算，hashmap中是不是也有相同
            return new GenericEventExecutorChooser(executors);
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    /**
     * 2的次幂获取
     */
    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {

        /**
         * 索引下标
         */
        private final AtomicInteger idx = new AtomicInteger();

        /**
         * 一组执行器
         */
        private final EventExecutor[] executors;

        public PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            // idx初始化为0，之后每一次调用next方法，idx都会自增，这样经过运算后，得到的数组下标就会成为一个循环，执行器也就会被循环获取
            // 也就是轮询
            return executors[idx.getAndIncrement() & executors.length - 1];
        }
    }

    /**
     * 通用的获取
     */
    private static final class GenericEventExecutorChooser implements EventExecutorChooser {

        /**
         * 索引下标
         */
        private final AtomicInteger idx = new AtomicInteger();

        /**
         * 一组执行器
         */
        private final EventExecutor[] executors;

        GenericEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            return executors[Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }
}
