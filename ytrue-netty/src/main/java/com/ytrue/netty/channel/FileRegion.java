package com.ytrue.netty.channel;

import com.ytrue.netty.util.ReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * @author ytrue
 * @date 2023-08-13 9:35
 * @description FileRegion
 */
public interface FileRegion extends ReferenceCounted {


    long position();


    @Deprecated
    long transfered();


    long transferred();


    long count();


    long transferTo(WritableByteChannel target, long position) throws IOException;

    @Override
    FileRegion retain();

    @Override
    FileRegion retain(int increment);

    @Override
    FileRegion touch();

    @Override
    FileRegion touch(Object hint);
}
