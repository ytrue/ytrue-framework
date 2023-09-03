package com.ytrue.job.admin.core.alarm;

import com.ytrue.job.admin.core.model.XxlJobInfo;
import com.ytrue.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-03 9:48
 * @description 这个类是用来发送报警邮件的，但实际上真正的功能并不在这个类实现，而是在EmailJobAlarm类实现
 */
@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);
    /**
     * Springboot容器
     */
    private ApplicationContext applicationContext;
    /**
     * 邮件报警器的集合
     */
    private List<JobAlarm> jobAlarmList;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        //把容器中所有的邮件报警器收集到jobAlarmList集合中
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
            jobAlarmList = new ArrayList<>(serviceBeanMap.values());
        }
    }


    /**
     * 在JobFailMonitorHelper类中被调用到的发送报警邮件的方法
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean alarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean result = false;
        //先判断邮件报警器集合是否为空
        if (jobAlarmList != null && jobAlarmList.size() > 0) {
            //不为空就先设置所有报警器发送结果都为成功
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                //遍历邮件报警器，然后设置发送结果为false
                boolean resultItem = false;
                try {
                    //在这里真正发送报警邮件给用户，然后返回给用户发送结果
                    resultItem = alarm.doAlarm(info, jobLog);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    //这里可以看到，如果发送失败，就把最开始设置的result重新改为false
                    //并且这里可以明白，只要有一个报警器发送邮件失败，总的发送结果就会被设置为失败
                    result = false;
                }
            }
        }
        return result;
    }
}
