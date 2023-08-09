package com.ytrue.netty.buffer;

import com.ytrue.netty.util.ByteProcessor;
import com.ytrue.netty.util.CharsetUtil;
import com.ytrue.netty.util.internal.PlatformDependent;
import com.ytrue.netty.util.internal.StringUtil;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

import static com.ytrue.netty.util.internal.MathUtil.isOutOfBounds;
import static com.ytrue.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author ytrue
 * @date 2023-08-07 11:25
 * @description 该抽象类中定义了所有的对字节或其他基本数据类型的方法。向ByteBuf写入数据或者是读取数据的方法，都在该方法内被
 * 定义了。不同的实现类直接调用该模版即可
 */
public abstract class AbstractByteBuf extends ByteBuf {
    private static final Logger logger = LoggerFactory.getLogger(AbstractByteBuf.class);
    private static final String LEGACY_PROP_CHECK_ACCESSIBLE = "io.netty.buffer.bytebuf.checkAccessible";
    private static final String PROP_CHECK_ACCESSIBLE = "io.netty.buffer.checkAccessible";
    static final boolean checkAccessible; // accessed from CompositeByteBuf
    private static final String PROP_CHECK_BOUNDS = "io.netty.buffer.checkBounds";
    private static final boolean checkBounds;

    static {
        if (SystemPropertyUtil.contains(PROP_CHECK_ACCESSIBLE)) {
            checkAccessible = SystemPropertyUtil.getBoolean(PROP_CHECK_ACCESSIBLE, true);
        } else {
            checkAccessible = SystemPropertyUtil.getBoolean(LEGACY_PROP_CHECK_ACCESSIBLE, true);
        }
        checkBounds = SystemPropertyUtil.getBoolean(PROP_CHECK_BOUNDS, true);
        if (logger.isDebugEnabled()) {
            logger.debug("-D{}: {}", PROP_CHECK_ACCESSIBLE, checkAccessible);
            logger.debug("-D{}: {}", PROP_CHECK_BOUNDS, checkBounds);
        }
    }

//    static final ResourceLeakDetector<ByteBuf> leakDetector =
//            ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ByteBuf.class);

    int readerIndex;
    int writerIndex;
    private int markedReaderIndex;
    private int markedWriterIndex;
    private int maxCapacity;

    protected AbstractByteBuf(int maxCapacity) {
        checkPositiveOrZero(maxCapacity, "maxCapacity");
        this.maxCapacity = maxCapacity;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ByteBuf asReadOnly() {
//        if (isReadOnly()) {
//            return this;
//        }
//        return Unpooled.unmodifiableBuffer(this);
        return this;
    }

    @Override
    public int maxCapacity() {
        return maxCapacity;
    }

    protected final void maxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public int readerIndex() {
        return readerIndex;
    }

    private static void checkIndexBounds(final int readerIndex, final int writerIndex, final int capacity) {
        if (readerIndex < 0 || readerIndex > writerIndex || writerIndex > capacity) {
            throw new IndexOutOfBoundsException(String.format(
                    "readerIndex: %d, writerIndex: %d (expected: 0 <= readerIndex <= writerIndex <= capacity(%d))",
                    readerIndex, writerIndex, capacity));
        }
    }

    @Override
    public ByteBuf readerIndex(int readerIndex) {
        if (checkBounds) {
            checkIndexBounds(readerIndex, writerIndex, capacity());
        }
        this.readerIndex = readerIndex;
        return this;
    }

    @Override
    public int writerIndex() {
        return writerIndex;
    }

    @Override
    public ByteBuf writerIndex(int writerIndex) {
        if (checkBounds) {
            checkIndexBounds(readerIndex, writerIndex, capacity());
        }
        this.writerIndex = writerIndex;
        return this;
    }

    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        if (checkBounds) {
            checkIndexBounds(readerIndex, writerIndex, capacity());
        }
        setIndex0(readerIndex, writerIndex);
        return this;
    }

    @Override
    public ByteBuf clear() {
        readerIndex = writerIndex = 0;
        return this;
    }

    @Override
    public boolean isReadable() {
        return writerIndex > readerIndex;
    }

    @Override
    public boolean isReadable(int numBytes) {
        return writerIndex - readerIndex >= numBytes;
    }

    @Override
    public boolean isWritable() {
        return capacity() > writerIndex;
    }

    @Override
    public boolean isWritable(int numBytes) {
        return capacity() - writerIndex >= numBytes;
    }

    @Override
    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    @Override
    public int writableBytes() {
        return capacity() - writerIndex;
    }

    @Override
    public int maxWritableBytes() {
        return maxCapacity() - writerIndex;
    }

    @Override
    public ByteBuf markReaderIndex() {
        markedReaderIndex = readerIndex;
        return this;
    }

    @Override
    public ByteBuf resetReaderIndex() {
        readerIndex(markedReaderIndex);
        return this;
    }

    @Override
    public ByteBuf markWriterIndex() {
        markedWriterIndex = writerIndex;
        return this;
    }

    @Override
    public ByteBuf resetWriterIndex() {
        writerIndex(markedWriterIndex);
        return this;
    }

    @Override
    public ByteBuf discardReadBytes() {
        ensureAccessible();
        if (readerIndex == 0) {
            return this;
        }

        if (readerIndex != writerIndex) {
            setBytes(0, this, readerIndex, writerIndex - readerIndex);
            writerIndex -= readerIndex;
            adjustMarkers(readerIndex);
            readerIndex = 0;
        } else {
            adjustMarkers(readerIndex);
            writerIndex = readerIndex = 0;
        }
        return this;
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        ensureAccessible();
        if (readerIndex == 0) {
            return this;
        }

        if (readerIndex == writerIndex) {
            adjustMarkers(readerIndex);
            writerIndex = readerIndex = 0;
            return this;
        }

        if (readerIndex >= capacity() >>> 1) {
            setBytes(0, this, readerIndex, writerIndex - readerIndex);
            writerIndex -= readerIndex;
            adjustMarkers(readerIndex);
            readerIndex = 0;
        }
        return this;
    }

    protected final void adjustMarkers(int decrement) {
        int markedReaderIndex = this.markedReaderIndex;
        if (markedReaderIndex <= decrement) {
            this.markedReaderIndex = 0;
            int markedWriterIndex = this.markedWriterIndex;
            if (markedWriterIndex <= decrement) {
                this.markedWriterIndex = 0;
            } else {
                this.markedWriterIndex = markedWriterIndex - decrement;
            }
        } else {
            this.markedReaderIndex = markedReaderIndex - decrement;
            markedWriterIndex -= decrement;
        }
    }

    @Override
    public ByteBuf ensureWritable(int minWritableBytes) {
        checkPositiveOrZero(minWritableBytes, "minWritableBytes");
        ensureWritable0(minWritableBytes);
        return this;
    }

    final void ensureWritable0(int minWritableBytes) {
        ensureAccessible();
        if (minWritableBytes <= writableBytes()) {
            return;
        }
        final int writerIndex = writerIndex();
        if (checkBounds) {
            if (minWritableBytes > maxCapacity - writerIndex) {
                throw new IndexOutOfBoundsException(String.format(
                        "writerIndex(%d) + minWritableBytes(%d) exceeds maxCapacity(%d): %s",
                        writerIndex, minWritableBytes, maxCapacity, this));
            }
        }

        // Normalize the current capacity to the power of 2.
        int minNewCapacity = writerIndex + minWritableBytes;
        int newCapacity = alloc().calculateNewCapacity(minNewCapacity, maxCapacity);

        int fastCapacity = writerIndex + maxFastWritableBytes();
        // Grow by a smaller amount if it will avoid reallocation
        if (newCapacity > fastCapacity && minNewCapacity <= fastCapacity) {
            newCapacity = fastCapacity;
        }

        // Adjust to the new capacity.
        capacity(newCapacity);
    }

    @Override
    public int ensureWritable(int minWritableBytes, boolean force) {
        ensureAccessible();
        checkPositiveOrZero(minWritableBytes, "minWritableBytes");

        if (minWritableBytes <= writableBytes()) {
            return 0;
        }

        final int maxCapacity = maxCapacity();
        final int writerIndex = writerIndex();
        if (minWritableBytes > maxCapacity - writerIndex) {
            if (!force || capacity() == maxCapacity) {
                return 1;
            }

            capacity(maxCapacity);
            return 3;
        }

        // Normalize the current capacity to the power of 2.
        int minNewCapacity = writerIndex + minWritableBytes;
        int newCapacity = alloc().calculateNewCapacity(minNewCapacity, maxCapacity);

        int fastCapacity = writerIndex + maxFastWritableBytes();
        // Grow by a smaller amount if it will avoid reallocation
        if (newCapacity > fastCapacity && minNewCapacity <= fastCapacity) {
            newCapacity = fastCapacity;
        }

        // Adjust to the new capacity.
        capacity(newCapacity);
        return 2;
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        if (endianness == order()) {
            return this;
        }
        if (endianness == null) {
            throw new NullPointerException("endianness");
        }
        //return newSwappedByteBuf();
        return null;
    }

    //该类已经被废弃，所以我就直接注释掉了
//    protected SwappedByteBuf newSwappedByteBuf() {
//        return new SwappedByteBuf(this);
//    }

    @Override
    public byte getByte(int index) {
        checkIndex(index);
        return _getByte(index);
    }

    protected abstract byte _getByte(int index);

    @Override
    public boolean getBoolean(int index) {
        return getByte(index) != 0;
    }

    @Override
    public short getUnsignedByte(int index) {
        return (short) (getByte(index) & 0xFF);
    }

    @Override
    public short getShort(int index) {
        checkIndex(index, 2);
        return _getShort(index);
    }

    protected abstract short _getShort(int index);

    @Override
    public short getShortLE(int index) {
        checkIndex(index, 2);
        return _getShortLE(index);
    }

    protected abstract short _getShortLE(int index);

    @Override
    public int getUnsignedShort(int index) {
        return getShort(index) & 0xFFFF;
    }

    @Override
    public int getUnsignedShortLE(int index) {
        return getShortLE(index) & 0xFFFF;
    }

    @Override
    public int getUnsignedMedium(int index) {
        checkIndex(index, 3);
        return _getUnsignedMedium(index);
    }

    protected abstract int _getUnsignedMedium(int index);

    @Override
    public int getUnsignedMediumLE(int index) {
        checkIndex(index, 3);
        return _getUnsignedMediumLE(index);
    }

    protected abstract int _getUnsignedMediumLE(int index);

    @Override
    public int getMedium(int index) {
        int value = getUnsignedMedium(index);
        if ((value & 0x800000) != 0) {
            value |= 0xff000000;
        }
        return value;
    }

    @Override
    public int getMediumLE(int index) {
        int value = getUnsignedMediumLE(index);
        if ((value & 0x800000) != 0) {
            value |= 0xff000000;
        }
        return value;
    }

    @Override
    public int getInt(int index) {
        checkIndex(index, 4);
        return _getInt(index);
    }

    protected abstract int _getInt(int index);

    @Override
    public int getIntLE(int index) {
        checkIndex(index, 4);
        return _getIntLE(index);
    }

    protected abstract int _getIntLE(int index);

    @Override
    public long getUnsignedInt(int index) {
        return getInt(index) & 0xFFFFFFFFL;
    }

    @Override
    public long getUnsignedIntLE(int index) {
        return getIntLE(index) & 0xFFFFFFFFL;
    }

    @Override
    public long getLong(int index) {
        checkIndex(index, 8);
        return _getLong(index);
    }

    protected abstract long _getLong(int index);

    @Override
    public long getLongLE(int index) {
        checkIndex(index, 8);
        return _getLongLE(index);
    }

    protected abstract long _getLongLE(int index);

    @Override
    public char getChar(int index) {
        return (char) getShort(index);
    }

    @Override
    public float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }

    @Override
    public double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        getBytes(index, dst, 0, dst.length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        getBytes(index, dst, dst.writableBytes());
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        getBytes(index, dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
//        if (CharsetUtil.US_ASCII.equals(charset) || CharsetUtil.ISO_8859_1.equals(charset)) {
//            // ByteBufUtil.getBytes(...) will return a new copy which the AsciiString uses directly
//            return new AsciiString(ByteBufUtil.getBytes(this, index, length, true), false);
//        }
        return toString(index, length, charset);
    }

    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        CharSequence sequence = getCharSequence(readerIndex, length, charset);
        readerIndex += length;
        return sequence;
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        checkIndex(index);
        _setByte(index, value);
        return this;
    }

    protected abstract void _setByte(int index, int value);

    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        setByte(index, value ? 1 : 0);
        return this;
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        checkIndex(index, 2);
        _setShort(index, value);
        return this;
    }

    protected abstract void _setShort(int index, int value);

    @Override
    public ByteBuf setShortLE(int index, int value) {
        checkIndex(index, 2);
        _setShortLE(index, value);
        return this;
    }

    protected abstract void _setShortLE(int index, int value);

    @Override
    public ByteBuf setChar(int index, int value) {
        setShort(index, value);
        return this;
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        checkIndex(index, 3);
        _setMedium(index, value);
        return this;
    }

    protected abstract void _setMedium(int index, int value);

    @Override
    public ByteBuf setMediumLE(int index, int value) {
        checkIndex(index, 3);
        _setMediumLE(index, value);
        return this;
    }

    protected abstract void _setMediumLE(int index, int value);

    @Override
    public ByteBuf setInt(int index, int value) {
        checkIndex(index, 4);
        _setInt(index, value);
        return this;
    }

    protected abstract void _setInt(int index, int value);

    @Override
    public ByteBuf setIntLE(int index, int value) {
        checkIndex(index, 4);
        _setIntLE(index, value);
        return this;
    }

    protected abstract void _setIntLE(int index, int value);

    @Override
    public ByteBuf setFloat(int index, float value) {
        setInt(index, Float.floatToRawIntBits(value));
        return this;
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        checkIndex(index, 8);
        _setLong(index, value);
        return this;
    }

    protected abstract void _setLong(int index, long value);

    @Override
    public ByteBuf setLongLE(int index, long value) {
        checkIndex(index, 8);
        _setLongLE(index, value);
        return this;
    }

    protected abstract void _setLongLE(int index, long value);

    @Override
    public ByteBuf setDouble(int index, double value) {
        setLong(index, Double.doubleToRawLongBits(value));
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        setBytes(index, src, 0, src.length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        setBytes(index, src, src.readableBytes());
        return this;
    }

    private static void checkReadableBounds(final ByteBuf src, final int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException(String.format(
                    "length(%d) exceeds src.readableBytes(%d) where src is: %s", length, src.readableBytes(), src));
        }
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        checkIndex(index, length);
        if (src == null) {
            throw new NullPointerException("src");
        }
        if (checkBounds) {
            checkReadableBounds(src, length);
        }

        setBytes(index, src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        if (length == 0) {
            return this;
        }

        checkIndex(index, length);

        int nLong = length >>> 3;
        int nBytes = length & 7;
        for (int i = nLong; i > 0; i--) {
            _setLong(index, 0);
            index += 8;
        }
        if (nBytes == 4) {
            _setInt(index, 0);
            // Not need to update the index as we not will use it after this.
        } else if (nBytes < 4) {
            for (int i = nBytes; i > 0; i--) {
                _setByte(index, (byte) 0);
                index++;
            }
        } else {
            _setInt(index, 0);
            index += 4;
            for (int i = nBytes - 4; i > 0; i--) {
                _setByte(index, (byte) 0);
                index++;
            }
        }
        return this;
    }

    @Override
    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return setCharSequence0(index, sequence, charset, false);
    }

    private int setCharSequence0(int index, CharSequence sequence, Charset charset, boolean expand) {
        if (charset.equals(CharsetUtil.UTF_8)) {
            int length = ByteBufUtil.utf8MaxBytes(sequence);
            if (expand) {
                ensureWritable0(length);
                checkIndex0(index, length);
            } else {
                checkIndex(index, length);
            }
            return ByteBufUtil.writeUtf8(this, index, sequence, sequence.length());
        }
        if (charset.equals(CharsetUtil.US_ASCII) || charset.equals(CharsetUtil.ISO_8859_1)) {
            int length = sequence.length();
            if (expand) {
                ensureWritable0(length);
                checkIndex0(index, length);
            } else {
                checkIndex(index, length);
            }
            return ByteBufUtil.writeAscii(this, index, sequence, length);
        }
        byte[] bytes = sequence.toString().getBytes(charset);
        if (expand) {
            ensureWritable0(bytes.length);
            // setBytes(...) will take care of checking the indices.
        }
        setBytes(index, bytes);
        return bytes.length;
    }

    @Override
    public byte readByte() {
        checkReadableBytes0(1);
        int i = readerIndex;
        byte b = _getByte(i);
        readerIndex = i + 1;
        return b;
    }

    @Override
    public boolean readBoolean() {
        return readByte() != 0;
    }

    @Override
    public short readUnsignedByte() {
        return (short) (readByte() & 0xFF);
    }

    @Override
    public short readShort() {
        checkReadableBytes0(2);
        short v = _getShort(readerIndex);
        readerIndex += 2;
        return v;
    }

    @Override
    public short readShortLE() {
        checkReadableBytes0(2);
        short v = _getShortLE(readerIndex);
        readerIndex += 2;
        return v;
    }

    @Override
    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    @Override
    public int readUnsignedShortLE() {
        return readShortLE() & 0xFFFF;
    }

    @Override
    public int readMedium() {
        int value = readUnsignedMedium();
        if ((value & 0x800000) != 0) {
            value |= 0xff000000;
        }
        return value;
    }

    @Override
    public int readMediumLE() {
        int value = readUnsignedMediumLE();
        if ((value & 0x800000) != 0) {
            value |= 0xff000000;
        }
        return value;
    }

    @Override
    public int readUnsignedMedium() {
        checkReadableBytes0(3);
        int v = _getUnsignedMedium(readerIndex);
        readerIndex += 3;
        return v;
    }

    @Override
    public int readUnsignedMediumLE() {
        checkReadableBytes0(3);
        int v = _getUnsignedMediumLE(readerIndex);
        readerIndex += 3;
        return v;
    }

    @Override
    public int readInt() {
        checkReadableBytes0(4);
        int v = _getInt(readerIndex);
        readerIndex += 4;
        return v;
    }

    @Override
    public int readIntLE() {
        checkReadableBytes0(4);
        int v = _getIntLE(readerIndex);
        readerIndex += 4;
        return v;
    }

    @Override
    public long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    @Override
    public long readUnsignedIntLE() {
        return readIntLE() & 0xFFFFFFFFL;
    }

    @Override
    public long readLong() {
        checkReadableBytes0(8);
        long v = _getLong(readerIndex);
        readerIndex += 8;
        return v;
    }

    @Override
    public long readLongLE() {
        checkReadableBytes0(8);
        long v = _getLongLE(readerIndex);
        readerIndex += 8;
        return v;
    }

    @Override
    public char readChar() {
        return (char) readShort();
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public ByteBuf readBytes(int length) {
        checkReadableBytes(length);
//        if (length == 0) {
//            return Unpooled.EMPTY_BUFFER;
//        }
        ByteBuf buf = alloc().buffer(length, maxCapacity);
        buf.writeBytes(this, readerIndex, length);
        readerIndex += length;
        return buf;
    }

    @Override
    public ByteBuf readSlice(int length) {
        checkReadableBytes(length);
        ByteBuf slice = slice(readerIndex, length);
        readerIndex += length;
        return slice;
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        checkReadableBytes(length);
        ByteBuf slice = retainedSlice(readerIndex, length);
        readerIndex += length;
        return slice;
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        readBytes(dst, dst.writableBytes());
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        if (checkBounds) {
            if (length > dst.writableBytes()) {
                throw new IndexOutOfBoundsException(String.format(
                        "length(%d) exceeds dst.writableBytes(%d) where dst is: %s", length, dst.writableBytes(), dst));
            }
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        checkReadableBytes(length);
        getBytes(readerIndex, dst);
        readerIndex += length;
        return this;
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length)
            throws IOException {
        checkReadableBytes(length);
        int readBytes = getBytes(readerIndex, out, length);
        readerIndex += readBytes;
        return readBytes;
    }

    @Override
    public int readBytes(FileChannel out, long position, int length)
            throws IOException {
        checkReadableBytes(length);
        int readBytes = getBytes(readerIndex, out, position, length);
        readerIndex += readBytes;
        return readBytes;
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        checkReadableBytes(length);
        getBytes(readerIndex, out, length);
        readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf skipBytes(int length) {
        checkReadableBytes(length);
        readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBoolean(boolean value) {
        writeByte(value ? 1 : 0);
        return this;
    }

    @Override
    public ByteBuf writeByte(int value) {
        ensureWritable0(1);
        _setByte(writerIndex++, value);
        return this;
    }

    @Override
    public ByteBuf writeShort(int value) {
        ensureWritable0(2);
        _setShort(writerIndex, value);
        writerIndex += 2;
        return this;
    }

    @Override
    public ByteBuf writeShortLE(int value) {
        ensureWritable0(2);
        _setShortLE(writerIndex, value);
        writerIndex += 2;
        return this;
    }

    @Override
    public ByteBuf writeMedium(int value) {
        ensureWritable0(3);
        _setMedium(writerIndex, value);
        writerIndex += 3;
        return this;
    }

    @Override
    public ByteBuf writeMediumLE(int value) {
        ensureWritable0(3);
        _setMediumLE(writerIndex, value);
        writerIndex += 3;
        return this;
    }

    @Override
    public ByteBuf writeInt(int value) {
        ensureWritable0(4);
        _setInt(writerIndex, value);
        writerIndex += 4;
        return this;
    }

    @Override
    public ByteBuf writeIntLE(int value) {
        ensureWritable0(4);
        _setIntLE(writerIndex, value);
        writerIndex += 4;
        return this;
    }

    @Override
    public ByteBuf writeLong(long value) {
        ensureWritable0(8);
        _setLong(writerIndex, value);
        writerIndex += 8;
        return this;
    }

    @Override
    public ByteBuf writeLongLE(long value) {
        ensureWritable0(8);
        _setLongLE(writerIndex, value);
        writerIndex += 8;
        return this;
    }

    @Override
    public ByteBuf writeChar(int value) {
        writeShort(value);
        return this;
    }

    @Override
    public ByteBuf writeFloat(float value) {
        writeInt(Float.floatToRawIntBits(value));
        return this;
    }

    @Override
    public ByteBuf writeDouble(double value) {
        writeLong(Double.doubleToRawLongBits(value));
        return this;
    }

    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {

        // ensureWritable()  方法是 Netty 中的一个方法，用于确保缓冲区（Buffer）可写。当需要向缓冲区写入数据时，可以使用  ensureWritable()
        // 方法来检查缓冲区是否具有足够的可写空间，如果没有足够的空间，则会自动扩展缓冲区的容量以满足写入需求。
        // 该方法的具体行为如下：
        //- 如果缓冲区的可写空间足够， ensureWritable()  方法不会进行任何操作，直接返回。
        //- 如果缓冲区的可写空间不足， ensureWritable()  方法会自动扩展缓冲区的容量。扩展后的容量取决于具体的实现和策略。
        //- 扩展容量后，原有的数据将会被复制到新的缓冲区中，以保留之前已写入的数据。
        //- 扩展容量后，可以继续向缓冲区写入数据，确保有足够的可写空间。
        // 使用  ensureWritable()  方法可以确保在写入数据时不会出现缓冲区溢出的情况，提高代码的健壮性和可靠性。
        // 请注意，具体的实现和行为可能会因 Netty 版本和使用的具体类而有所不同。建议查阅相关文档或参考具体的使用示例以获取更准确和详细的信息。
        ensureWritable(length);
        setBytes(writerIndex, src, srcIndex, length);
        writerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBytes(byte[] src) {
        writeBytes(src, 0, src.length);
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        writeBytes(src, src.readableBytes());
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        if (checkBounds) {
            checkReadableBounds(src, length);
        }
        writeBytes(src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        ensureWritable(length);
        setBytes(writerIndex, src, srcIndex, length);
        writerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer src) {
        int length = src.remaining();
        ensureWritable0(length);
        setBytes(writerIndex, src);
        writerIndex += length;
        return this;
    }

    @Override
    public int writeBytes(InputStream in, int length)
            throws IOException {
        ensureWritable(length);
        int writtenBytes = setBytes(writerIndex, in, length);
        if (writtenBytes > 0) {
            writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        ensureWritable(length);
        int writtenBytes = setBytes(writerIndex, in, length);
        if (writtenBytes > 0) {
            writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    @Override
    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        ensureWritable(length);
        int writtenBytes = setBytes(writerIndex, in, position, length);
        if (writtenBytes > 0) {
            writerIndex += writtenBytes;
        }
        return writtenBytes;
    }

    @Override
    public ByteBuf writeZero(int length) {
        if (length == 0) {
            return this;
        }

        ensureWritable(length);
        int wIndex = writerIndex;
        checkIndex0(wIndex, length);

        int nLong = length >>> 3;
        int nBytes = length & 7;
        for (int i = nLong; i > 0; i--) {
            _setLong(wIndex, 0);
            wIndex += 8;
        }
        if (nBytes == 4) {
            _setInt(wIndex, 0);
            wIndex += 4;
        } else if (nBytes < 4) {
            for (int i = nBytes; i > 0; i--) {
                _setByte(wIndex, (byte) 0);
                wIndex++;
            }
        } else {
            _setInt(wIndex, 0);
            wIndex += 4;
            for (int i = nBytes - 4; i > 0; i--) {
                _setByte(wIndex, (byte) 0);
                wIndex++;
            }
        }
        writerIndex = wIndex;
        return this;
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        int written = setCharSequence0(writerIndex, sequence, charset, true);
        writerIndex += written;
        return written;
    }

    @Override
    public ByteBuf copy() {
        return copy(readerIndex, readableBytes());
    }

    //UnpooledDuplicatedByteBuf还未引入，暂时注释掉
    @Override
    public ByteBuf duplicate() {
        ensureAccessible();
        //return new UnpooledDuplicatedByteBuf(this);
        return null;
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return duplicate().retain();
    }

    @Override
    public ByteBuf slice() {
        return slice(readerIndex, readableBytes());
    }

    @Override
    public ByteBuf retainedSlice() {
        return slice().retain();
    }

    //UnpooledSlicedByteBuf还未引入，暂时注释掉
    @Override
    public ByteBuf slice(int index, int length) {
        ensureAccessible();
        //return new UnpooledSlicedByteBuf(this, index, length);
        return null;
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return slice(index, length).retain();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return nioBuffer(readerIndex, readableBytes());
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return nioBuffers(readerIndex, readableBytes());
    }

    @Override
    public String toString(Charset charset) {
        return toString(readerIndex, readableBytes(), charset);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        return ByteBufUtil.decodeString(this, index, length, charset);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return ByteBufUtil.indexOf(this, fromIndex, toIndex, value);
    }

    @Override
    public int bytesBefore(byte value) {
        return bytesBefore(readerIndex(), readableBytes(), value);
    }

    @Override
    public int bytesBefore(int length, byte value) {
        checkReadableBytes(length);
        return bytesBefore(readerIndex(), length, value);
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        int endIndex = indexOf(index, index + length, value);
        if (endIndex < 0) {
            return -1;
        }
        return endIndex - index;
    }

    @Override
    public int forEachByte(ByteProcessor processor) {
        ensureAccessible();
        try {
            return forEachByteAsc0(readerIndex, writerIndex, processor);
        } catch (Exception e) {
            PlatformDependent.throwException(e);
            return -1;
        }
    }

    @Override
    public int forEachByte(int index, int length, ByteProcessor processor) {
        checkIndex(index, length);
        try {
            return forEachByteAsc0(index, index + length, processor);
        } catch (Exception e) {
            PlatformDependent.throwException(e);
            return -1;
        }
    }

    int forEachByteAsc0(int start, int end, ByteProcessor processor) throws Exception {
        for (; start < end; ++start) {
            if (!processor.process(_getByte(start))) {
                return start;
            }
        }

        return -1;
    }

    @Override
    public int forEachByteDesc(ByteProcessor processor) {
        ensureAccessible();
        try {
            return forEachByteDesc0(writerIndex - 1, readerIndex, processor);
        } catch (Exception e) {
            PlatformDependent.throwException(e);
            return -1;
        }
    }

    @Override
    public int forEachByteDesc(int index, int length, ByteProcessor processor) {
        checkIndex(index, length);
        try {
            return forEachByteDesc0(index + length - 1, index, processor);
        } catch (Exception e) {
            PlatformDependent.throwException(e);
            return -1;
        }
    }

    int forEachByteDesc0(int rStart, final int rEnd, ByteProcessor processor) throws Exception {
        for (; rStart >= rEnd; --rStart) {
            if (!processor.process(_getByte(rStart))) {
                return rStart;
            }
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return ByteBufUtil.hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ByteBuf && ByteBufUtil.equals(this, (ByteBuf) o));
    }

    @Override
    public int compareTo(ByteBuf that) {
        return ByteBufUtil.compare(this, that);
    }

    @Override
    public String toString() {
        if (refCnt() == 0) {
            return StringUtil.simpleClassName(this) + "(freed)";
        }

        StringBuilder buf = new StringBuilder()
                .append(StringUtil.simpleClassName(this))
                .append("(ridx: ").append(readerIndex)
                .append(", widx: ").append(writerIndex)
                .append(", cap: ").append(capacity());
        if (maxCapacity != Integer.MAX_VALUE) {
            buf.append('/').append(maxCapacity);
        }

        ByteBuf unwrapped = unwrap();
        if (unwrapped != null) {
            buf.append(", unwrapped: ").append(unwrapped);
        }
        buf.append(')');
        return buf.toString();
    }

    protected final void checkIndex(int index) {
        checkIndex(index, 1);
    }

    protected final void checkIndex(int index, int fieldLength) {
        ensureAccessible();
        checkIndex0(index, fieldLength);
    }

    private static void checkRangeBounds(final String indexName, final int index,
                                         final int fieldLength, final int capacity) {
        if (isOutOfBounds(index, fieldLength, capacity)) {
            throw new IndexOutOfBoundsException(String.format(
                    "%s: %d, length: %d (expected: range(0, %d))", indexName, index, fieldLength, capacity));
        }
    }

    final void checkIndex0(int index, int fieldLength) {
        if (checkBounds) {
            checkRangeBounds("index", index, fieldLength, capacity());
        }
    }

    protected final void checkSrcIndex(int index, int length, int srcIndex, int srcCapacity) {
        checkIndex(index, length);
        if (checkBounds) {
            checkRangeBounds("srcIndex", srcIndex, length, srcCapacity);
        }
    }

    protected final void checkDstIndex(int index, int length, int dstIndex, int dstCapacity) {
        checkIndex(index, length);
        if (checkBounds) {
            checkRangeBounds("dstIndex", dstIndex, length, dstCapacity);
        }
    }

    protected final void checkDstIndex(int length, int dstIndex, int dstCapacity) {
        checkReadableBytes(length);
        if (checkBounds) {
            checkRangeBounds("dstIndex", dstIndex, length, dstCapacity);
        }
    }


    protected final void checkReadableBytes(int minimumReadableBytes) {
        checkPositiveOrZero(minimumReadableBytes, "minimumReadableBytes");
        checkReadableBytes0(minimumReadableBytes);
    }

    protected final void checkNewCapacity(int newCapacity) {
        ensureAccessible();
        if (checkBounds) {
            if (newCapacity < 0 || newCapacity > maxCapacity()) {
                throw new IllegalArgumentException("newCapacity: " + newCapacity +
                        " (expected: 0-" + maxCapacity() + ')');
            }
        }
    }

    private void checkReadableBytes0(int minimumReadableBytes) {
        ensureAccessible();
        if (checkBounds) {
            if (readerIndex > writerIndex - minimumReadableBytes) {
                throw new IndexOutOfBoundsException(String.format(
                        "readerIndex(%d) + length(%d) exceeds writerIndex(%d): %s",
                        readerIndex, minimumReadableBytes, writerIndex, this));
            }
        }
    }


    protected final void ensureAccessible() {
        if (checkAccessible && !isAccessible()) {
            throw new RuntimeException();
        }
    }

    final void setIndex0(int readerIndex, int writerIndex) {
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
    }

    final void discardMarks() {
        markedReaderIndex = markedWriterIndex = 0;
    }
}
