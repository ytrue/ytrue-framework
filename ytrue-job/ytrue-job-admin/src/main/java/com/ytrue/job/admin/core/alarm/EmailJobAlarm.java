package com.ytrue.job.admin.core.alarm;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.model.XxlJobGroup;
import com.ytrue.job.admin.core.model.XxlJobInfo;
import com.ytrue.job.admin.core.model.XxlJobLog;
import com.ytrue.job.admin.core.util.I18nUtil;
import com.ytrue.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-09-03 9:45
 * @description EmailJobAlarm
 */
@Component
public class EmailJobAlarm implements JobAlarm {

    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean alarmResult = true;
        //做一些判空校验
        if (info != null && info.getAlarmEmail() != null && info.getAlarmEmail().trim().length() > 0) {
            //得到报警的定时任务的id
            String alarmContent = "Alarm Job LogId=" + jobLog.getId();
            //下面注释我就不详细写了，都很简单，都是结果内容的设置，大家应该都对这些很熟悉了
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>TriggerMsg=<br>" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>HandleCode=" + jobLog.getHandleMsg();
            }

            //得到执行器组
            XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(Integer.valueOf(info.getJobGroup()));

            //设置报警信息的发送者，具体值大家可以去I18nUtil的配置文件中查看
            String personal = I18nUtil.getString("admin_name_full");
            //设置报警信息的标题
            String title = I18nUtil.getString("jobconf_monitor");
            //向模版中填充具体的内容，就不具体解释了，都很简单
            String content = MessageFormat.format(loadEmailJobAlarmTemplate(),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobDesc(),
                    alarmContent);
            //也许设置了多个邮件地址，所以这里把它转化为集合
            Set<String> emailSet = new HashSet<>(Arrays.asList(info.getAlarmEmail().split(",")));
            //遍历地址，然后就是给每一个地址发送报警邮件了
            for (String email : emailSet) {
                try {
                    //下面这些步骤就不用详细解释了吧，这些都是常规流程了，用过mail的jar包都很熟悉了吧
                    MimeMessage mimeMessage = XxlJobAdminConfig.getAdminConfig().getMailSender().createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(XxlJobAdminConfig.getAdminConfig().getEmailFrom(), personal);
                    helper.setTo(email);
                    helper.setSubject(title);
                    helper.setText(content, true);
                    XxlJobAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>> xxl-job, job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);
                    alarmResult = false;
                }
            }
        }
        //返回发送结果
        return alarmResult;
    }


    /**
     * 这个是前端要用到的模版
     *
     * @return
     */
    private static String loadEmailJobAlarmTemplate() {
        String mailBodyTemplate = "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>" +
                                  "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                                  "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
                                  "      <tr>\n" +
                                  "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobgroup") + "</td>\n" +
                                  "         <td width=\"10%\" >" + I18nUtil.getString("jobinfo_field_id") + "</td>\n" +
                                  "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobdesc") + "</td>\n" +
                                  "         <td width=\"10%\" >" + I18nUtil.getString("jobconf_monitor_alarm_title") + "</td>\n" +
                                  "         <td width=\"40%\" >" + I18nUtil.getString("jobconf_monitor_alarm_content") + "</td>\n" +
                                  "      </tr>\n" +
                                  "   </thead>\n" +
                                  "   <tbody>\n" +
                                  "      <tr>\n" +
                                  "         <td>{0}</td>\n" +
                                  "         <td>{1}</td>\n" +
                                  "         <td>{2}</td>\n" +
                                  "         <td>" + I18nUtil.getString("jobconf_monitor_alarm_type") + "</td>\n" +
                                  "         <td>{3}</td>\n" +
                                  "      </tr>\n" +
                                  "   </tbody>\n" +
                                  "</table>";
        return mailBodyTemplate;
    }
}
