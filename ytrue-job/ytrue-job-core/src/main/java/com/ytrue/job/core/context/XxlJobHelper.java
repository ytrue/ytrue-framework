package com.ytrue.job.core.context;

import com.ytrue.job.core.log.XxlJobFileAppender;
import com.ytrue.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * @author ytrue
 * @date 2023-09-01 10:03
 * @description 这个类的功能就是对日志进行处理
 */
public class XxlJobHelper {

    /**
     * 从threadLocal获取定时任务的id
     *
     * @return
     */
    public static long getJobId() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getJobId();
    }


    /**
     * 获取定时任务的执行参数
     *
     * @return
     */
    public static String getJobParam() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobParam();
    }


    /**
     * 获取定时任务的日志记录的文件名称
     *
     * @return
     */
    public static String getJobLogFileName() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getJobLogFileName();
    }


    /**
     * 获取分片索引，这里还用不到
     *
     * @return
     */
    public static int getShardIndex() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardIndex();
    }

    /**
     * 获取分片总数，这里也用不到
     *
     * @return
     */
    public static int getShardTotal() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardTotal();
    }


    private static final Logger logger = LoggerFactory.getLogger("xxl-job logger");


    /**
     * 存储定时任务日志的入口方法
     *
     * @param appendLogPattern
     * @param appendLogArguments
     * @return
     */
    public static boolean log(String appendLogPattern, Object... appendLogArguments) {
        // 2023-09-01 10:15:23 [com.ytrue.job.core.context.XxlJobHelper#main]-[115]-[main] test

        //该方法的作用是用来格式化要记录的日志信息
        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();
        //从栈帧中获得方法的调用信息
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        //在这里开始存储日志，但这里实际上只是个入口方法，真正的操作还是会进一步调用XxlJobFileAppender类的方法来完成的
        return logDetail(callInfo, appendLog);
    }

    /**
     * 该方法是用来把定时任务调用过程中遇到的异常记录到日志文件中
     *
     * @param e
     * @return
     */
    public static boolean log(Throwable e) {
        // 2023-09-01 10:17:02 [com.ytrue.job.core.context.XxlJobHelper#main]-[117]-[main] java.lang.RuntimeException: test error
        //	at com.ytrue.job.core.context.XxlJobHelper.main(XxlJobHelper.java:117)

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];

        return logDetail(callInfo, appendLog);
    }


    /**
     * 把定时任务的日志存储到日志文件中的方法
     *
     * @param callInfo
     * @param appendLog
     * @return
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        //从当前线程中获得定时任务上下文对象
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
//        if (xxlJobContext == null) {
//            return false;
//        }
        StringBuffer stringBuffer = new StringBuffer();
        //在这里把方法调用的详细信息拼接一下
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]").append("-")
                .append("[" + callInfo.getLineNumber() + "]").append("-")
                .append("[" + Thread.currentThread().getName() + "]").append(" ")
                .append(appendLog != null ? appendLog : "");
        //转换成字符串
        String formatAppendLog = stringBuffer.toString();

        System.out.println(formatAppendLog);

        //获取定时任务对应的日志存储路径
        String logFileName = xxlJobContext.getJobLogFileName();
        if (logFileName != null && logFileName.trim().length() > 0) {
            //真正存储日志的方法，在这里就把日志存储到本地文件了
            XxlJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }


    // 下面这几个方法作用都相同，都是把定时任务执行的结果信息保存到定时任务上下文对象中，大家简单看看就行了
    public static boolean handleSuccess() {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, null);
    }


    public static boolean handleSuccess(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, handleMsg);
    }


    public static boolean handleFail() {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, null);
    }


    public static boolean handleFail(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, handleMsg);
    }


    public static boolean handleTimeout() {
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, null);
    }


    public static boolean handleTimeout(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }


    public static boolean handleResult(int handleCode, String handleMsg) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }
        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }
}
