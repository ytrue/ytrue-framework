package com.ytrue.orm.session;

import lombok.Getter;

/**
 * @author ytrue
 * @date 2022/8/25 15:44
 * @description 分页记录限制
 */
public class RowBounds {

    /**
     * 默认的偏移量
     */
    public static final int NO_ROW_OFFSET = 0;

    /**
     * 默认的行数
     */
    public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;

    /**
     * 默认的分页记录限制
     */
    public static final RowBounds DEFAULT = new RowBounds();


    /**
     * offset,limit就等于一般分页的start,limit,
     */
    @Getter
    private int offset;
    @Getter
    private int limit;


    /**
     * 默认是一页Integer.MAX_VALUE条
     */
    public RowBounds() {
        this.offset = NO_ROW_OFFSET;
        this.limit = NO_ROW_LIMIT;
    }

    public RowBounds(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }
}
