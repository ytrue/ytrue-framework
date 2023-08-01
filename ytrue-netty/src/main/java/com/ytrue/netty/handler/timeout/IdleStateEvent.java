package com.ytrue.netty.handler.timeout;

import com.ytrue.netty.util.internal.ObjectUtil;

/**
 * @author ytrue
 * @date 2023-08-01 9:02
 * @description IdleStateEvent
 */
public class IdleStateEvent {

    /**
     * 表示首次读空闲事件，空闲状态为IdleState.READER_IDLE，首次空闲为true。
     */
    public static final IdleStateEvent FIRST_READER_IDLE_STATE_EVENT = new IdleStateEvent(IdleState.READER_IDLE, true);

    /**
     * 表示后续读空闲事件，空闲状态为IdleState.READER_IDLE，首次空闲为false。
     */
    public static final IdleStateEvent READER_IDLE_STATE_EVENT = new IdleStateEvent(IdleState.READER_IDLE, false);

    /**
     * 表示首次写空闲事件，空闲状态为IdleState.WRITER_IDLE，首次空闲为true。
     */
    public static final IdleStateEvent FIRST_WRITER_IDLE_STATE_EVENT = new IdleStateEvent(IdleState.WRITER_IDLE, true);

    /**
     * 表示后续写空闲事件，空闲状态为IdleState.WRITER_IDLE，首次空闲为false
     */
    public static final IdleStateEvent WRITER_IDLE_STATE_EVENT = new IdleStateEvent(IdleState.WRITER_IDLE, false);

    /**
     * 表示首次读写空闲事件，空闲状态为IdleState.ALL_IDLE，首次空闲为true。
     */
    public static final IdleStateEvent FIRST_ALL_IDLE_STATE_EVENT = new IdleStateEvent(IdleState.ALL_IDLE, true);

    /**
     * 表示后续读写空闲事件，空闲状态为IdleState.ALL_IDLE，首次空闲为false
     */
    public static final IdleStateEvent ALL_IDLE_STATE_EVENT = new IdleStateEvent(IdleState.ALL_IDLE, false);

    /**
     * 获取空闲状态
     */
    private final IdleState state;

    /**
     * 否是首次进入空闲状态。这样可以区分首次进入空闲状态和后续的空闲状态。
     */
    private final boolean first;


    protected IdleStateEvent(IdleState state, boolean first) {
        this.state = ObjectUtil.checkNotNull(state, "state");
        this.first = first;
    }


    public IdleState state() {
        return state;
    }


    public boolean isFirst() {
        return first;
    }

}
