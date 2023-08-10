package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-10 9:37
 * @description BooleanSupplier
 */
public interface BooleanSupplier {

    boolean get() throws Exception;


    BooleanSupplier FALSE_SUPPLIER = new BooleanSupplier() {
        @Override
        public boolean get() {
            return false;
        }
    };


    BooleanSupplier TRUE_SUPPLIER = new BooleanSupplier() {
        @Override
        public boolean get() {
            return true;
        }
    };
}
