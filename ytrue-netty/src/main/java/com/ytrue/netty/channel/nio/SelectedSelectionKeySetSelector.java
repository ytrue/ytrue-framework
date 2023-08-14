package com.ytrue.netty.channel.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-08-14 15:45
 * @description 这个是netty自定义selector，这个selector就是优化过的selector。其内部持有了原生的selector
 * 一些主要方法仍然是通过原生的selector来执行的。但是对key的操作，是对netty自定义的SelectedSelectionKeySet的操作
 */
final class SelectedSelectionKeySetSelector extends Selector {
    private final SelectedSelectionKeySet selectionKeys;
    private final Selector delegate;

    SelectedSelectionKeySetSelector(Selector delegate, SelectedSelectionKeySet selectionKeys) {
        this.delegate = delegate;
        this.selectionKeys = selectionKeys;
    }

    /**
     * @Author: PP-jessica
     * @Description:从这里可以看到，执行操作的时候，实际上还是原生的selector在执行操作 因为原生的selector赋值给了delegate
     */
    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public SelectorProvider provider() {
        return delegate.provider();
    }

    @Override
    public Set<SelectionKey> keys() {
        return delegate.keys();
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return delegate.selectedKeys();
    }

    /**
     * @Author: PP-jessica
     * @Description:从这里可以看到，每次执行的之前，都要调用一下selectionKeys.reset()方法，这个方法就是用来清空key数组的 因为要轮训下一次的IO事件了
     */
    @Override
    public int selectNow() throws IOException {
        selectionKeys.reset();
        return delegate.selectNow();
    }

    @Override
    public int select(long timeout) throws IOException {
        selectionKeys.reset();
        return delegate.select(timeout);
    }

    @Override
    public int select() throws IOException {
        selectionKeys.reset();
        return delegate.select();
    }

    @Override
    public Selector wakeup() {
        return delegate.wakeup();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
