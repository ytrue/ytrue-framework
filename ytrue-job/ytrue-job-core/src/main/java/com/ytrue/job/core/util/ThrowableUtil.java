package com.ytrue.job.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author ytrue
 * @date 2023-08-28 10:56
 * @description ThrowableUtil
 */
public class ThrowableUtil {

    public static String toString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String errorMsg = stringWriter.toString();
        return errorMsg;
    }

}
