package com.ytrue.job.core.log;

import com.ytrue.job.core.biz.model.LogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ytrue
 * @date 2023-09-01 9:40
 * @description 该类是操作日志的类，对日志文件进行操作的功能全部封装在该类中
 */
public class XxlJobFileAppender {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobFileAppender.class);


    /**
     * D:\data\applogs\xxl-job\jobhandler
     * 默认的日志存储的路径，但是在执行器启动的时候，该路径会被用户在配置文件中设置的路径取代
     */
    private static String logBasePath = "/data/applogs/xxl-job/jobhandler";


    /**
     * D:\data\applogs\xxl-job\jobhandler\gluesource
     * 下面这个会在web端在线编辑代码，执行定时任务的时候，用这个路径把用户编辑的代码记录下来
     * concat() 方法用于将指定的字符串参数连接到字符串上。
     */
    private static String glueSrcPath = logBasePath.concat("/gluesource");


    /**
     * 初始化存储日志文件路径的方法，非常简单，就不细讲了
     *
     * @param logPath
     */
    public static void initLogPath(String logPath) {
        // 如果logPath不为空 就拿logPath赋值logBasePath
        if (logPath != null && logPath.trim().length() > 0) {
            logBasePath = logPath;
        }

        // 创建file
        File logPathDir = new File(logBasePath);
        // 如果目录不存在就创建目录
        if (!logPathDir.exists()) {
            logPathDir.mkdirs();
        }
        // 获取路径
        logBasePath = logPathDir.getPath();


        // 创建glueBaseDir
        File glueBaseDir = new File(logPathDir, "gluesource");

        // 如果目录不存在就创建目录
        if (!glueBaseDir.exists()) {
            glueBaseDir.mkdirs();
        }
        // 获取路径
        glueSrcPath = glueBaseDir.getPath();
    }


    /**
     * 该方法会根据定时任务的触发时间和其对应的日志id创造一个文件名，这个日志id是在调度中心就创建好的通过触发器参数传递给执行器的
     *
     * @param triggerDate
     * @param logId
     * @return
     */
    public static String makeLogFileName(Date triggerDate, long logId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //这里的getLogPath()会得到存储日志的基础路径，就是用户在配置文件设置的那个路径
        // D:\data\applogs\xxl-job\jobhandler\2023-11-11
        File logFilePath = new File(getLogPath(), sdf.format(triggerDate));
        //如果目录不存在就创建目录
        if (!logFilePath.exists()) {
            logFilePath.mkdir();
        }
        //  D:\data\applogs\xxl-job\jobhandler\2023-11-11\1.log
        String logFileName = logFilePath.getPath()
                // \
                .concat(File.separator)
                // id
                .concat(String.valueOf(logId))
                // .log
                .concat(".log");
        return logFileName;
    }


    public static String getLogPath() {
        return logBasePath;
    }

    public static String getGlueSrcPath() {
        return glueSrcPath;
    }


    /**
     * 把日志记录到本地的日志文件中
     *
     * @param logFileName
     * @param appendLog
     */
    public static void appendLog(String logFileName, String appendLog) {
        if (logFileName == null || logFileName.trim().length() == 0) {
            return;
        }
        // 根据文件名字创建file
        File logFile = new File(logFileName);

        // 判断文件是否存在
        if (!logFile.exists()) {
            try {
                // 创建文件
                logFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        // 判断追加的内容
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";
        FileOutputStream fos = null;
        try {
            // 创建输出流，追加内容
            fos = new FileOutputStream(logFile, true);
            // 写
            fos.write(appendLog.getBytes("utf-8"));
            // 刷入
            fos.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    // 关闭
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }


    /**
     * 读取本地的日志文件内容，这个方法虽然有点长，但都是常规逻辑，就是最基础的读取文件的操作
     *
     * @param logFileName 文件名
     * @param fromLineNum 第几行开始
     * @return
     */
    public static LogResult readLog(String logFileName, int fromLineNum) {
        // 校验文件名
        if (logFileName == null || logFileName.trim().length() == 0) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        // 创建file
        File logFile = new File(logFileName);
        // 判断文件是否存在
        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }
        //  创建一个 StringBuffer ，用于存储读取到的日志内容。
        StringBuffer logContentBuffer = new StringBuffer();
        // 初始化 toLineNum 为0，用于记录读取到的行数。
        int toLineNum = 0;
        // 创建一个 LineNumberReader ，用于逐行读取日志文件。
        LineNumberReader reader = null;


        try {
            //
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(logFile), "utf-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                // 获取读取的行数
                toLineNum = reader.getLineNumber();
                // 在读取过程中，记录当前行数到 toLineNum 中，并检查是否达到从指定行数开始读取的条件。
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);
    }


    /**
     * 也是读取日志文件内容，一行一行地读
     *
     * @param logFile
     * @return
     */
    public static String readLines(File logFile) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "utf-8"));
            if (reader != null) {
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

}
