package com.ytrue.netty.channel;

import com.ytrue.netty.buffer.ByteBuf;
import com.ytrue.netty.buffer.ByteBufAllocator;
import com.ytrue.netty.util.UncheckedBooleanSupplier;
import com.ytrue.netty.util.internal.UnstableApi;

import static com.ytrue.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * @author ytrue
 * @date 2023-08-10 9:36
 * @description RecvByteBufAllocator
 */
public interface RecvByteBufAllocator {

    Handle newHandle();

    @Deprecated
    interface Handle {

        ByteBuf allocate(ByteBufAllocator alloc);


        int guess();

        void reset(ChannelConfig config);


        void incMessagesRead(int numMessages);


        void lastBytesRead(int bytes);


        int lastBytesRead();


        void attemptedBytesRead(int bytes);


        int attemptedBytesRead();

        boolean continueReading();


        void readComplete();
    }

    @SuppressWarnings("deprecation")
    @UnstableApi
    interface ExtendedHandle extends Handle {
        boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier);
    }


    class DelegatingHandle implements Handle {
        private final Handle delegate;

        public DelegatingHandle(Handle delegate) {
            this.delegate = checkNotNull(delegate, "delegate");
        }


        protected final Handle delegate() {
            return delegate;
        }

        @Override
        public ByteBuf allocate(ByteBufAllocator alloc) {
            return delegate.allocate(alloc);
        }

        @Override
        public int guess() {
            return delegate.guess();
        }

        @Override
        public void reset(ChannelConfig config) {
            delegate.reset(config);
        }

        @Override
        public void incMessagesRead(int numMessages) {
            delegate.incMessagesRead(numMessages);
        }

        @Override
        public void lastBytesRead(int bytes) {
            delegate.lastBytesRead(bytes);
        }

        @Override
        public int lastBytesRead() {
            return delegate.lastBytesRead();
        }

        @Override
        public boolean continueReading() {
            return delegate.continueReading();
        }

        @Override
        public int attemptedBytesRead() {
            return delegate.attemptedBytesRead();
        }

        @Override
        public void attemptedBytesRead(int bytes) {
            delegate.attemptedBytesRead(bytes);
        }

        @Override
        public void readComplete() {
            delegate.readComplete();
        }
    }
}
