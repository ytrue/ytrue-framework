package com.ytrue.rpc.future;

import com.ytrue.rpc.protocol.RpcResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CountDownLatch;

/**
 * @author ytrue
 * @date 2023-05-20 13:54
 * @description SyncWriteFuture
 */
public class SyncWriteFuture implements WriteFuture<RpcResponse> {

    /**
     * 计数器
     */
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * 开始时间戳
     */
    private final long begin = System.currentTimeMillis();
    /**
     * 超时时间
     */
    private long timeout;

    /**
     * 响应
     */
    private RpcResponse response;

    /**
     * 请求id
     */
    private final String requestId;

    /**
     * 结果
     */
    private boolean writeResult;

    /**
     * 异常
     */
    private Throwable cause;

    /**
     * 是否超时
     */
    private boolean isTimeout;

    public SyncWriteFuture(String requestId) {
        this.requestId = requestId;
    }

    public SyncWriteFuture(String requestId, long timeout) {
        this.requestId = requestId;
        this.timeout = timeout;
        writeResult = true;
        isTimeout = false;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public boolean isWriteSuccess() {
        return writeResult;
    }

    @Override
    public void setWriteResult(boolean result) {
        this.writeResult = result;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public RpcResponse response() {
        return response;
    }

    @Override
    public void setResponse(RpcResponse response) {
        this.response = response;
        // 计数器减一
        latch.countDown();
    }

    @Override
    public boolean isTimeout() {
        if (isTimeout) {
            return true;
        }
        return System.currentTimeMillis() - begin > timeout;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public RpcResponse get() throws InterruptedException, ExecutionException {
        latch.wait();
        return response;
    }

    @Override
    public RpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            return response;
        }
        return null;
    }
}
