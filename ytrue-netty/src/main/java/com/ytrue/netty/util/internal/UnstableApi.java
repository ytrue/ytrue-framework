package com.ytrue.netty.util.internal;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2023-08-03 11:13
 * @description UnstableApi
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PACKAGE,
        ElementType.TYPE
})
@Documented
public @interface UnstableApi {
}
