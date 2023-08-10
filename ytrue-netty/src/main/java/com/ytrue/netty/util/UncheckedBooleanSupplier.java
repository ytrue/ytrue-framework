package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-10 9:37
 * @description UncheckedBooleanSupplier
 */
public interface UncheckedBooleanSupplier extends BooleanSupplier {

    @Override
    boolean get();


    UncheckedBooleanSupplier FALSE_SUPPLIER = new UncheckedBooleanSupplier() {
        @Override
        public boolean get() {
            return false;
        }
    };


    UncheckedBooleanSupplier TRUE_SUPPLIER = new UncheckedBooleanSupplier() {
        @Override
        public boolean get() {
            return true;
        }
    };
}
