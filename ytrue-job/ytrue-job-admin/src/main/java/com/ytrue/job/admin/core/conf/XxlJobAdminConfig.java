package com.ytrue.job.admin.core.conf;

import com.ytrue.job.admin.core.scheduler.XxlJobScheduler;
import com.ytrue.job.admin.dao.*;
import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description 这个类可以说是服务端的启动入口，该类实现了Spring的InitializingBean接口，所以该类中的
 * afterPropertiesSet方法会在容器中的bean初始化完毕后被回掉。回掉的过程中会创建xxl-job中最重要的块慢线程池
 * 同时也会启动xxl-job中的时间轮
 */
@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {


    /**
     * 当前类的引用，看到这里就应该想到单例模式了
     * xxl-job中所有组件都是用了单例模式，通过public的静态方法把对象暴露出去
     */
    @Getter
    private static XxlJobAdminConfig adminConfig = null;


    // 下面就是一些简单的属性注入，spring为我们做的-------------
    /**
     * 国际化
     */
    @Getter
    @Value("${xxl.job.i18n}")
    private String i18n;

    @Value("${xxl.job.accessToken}")
    @Getter
    private String accessToken;

    @Value("${spring.mail.from}")
    @Getter
    private String emailFrom;

    /**
     * 该属性是日志保留时间的意思
     */
    @Getter
    @Value("${xxl.job.logretentiondays}")
    private int logretentiondays;

    /**
     * 快线程池的最大线程数
     */
    @Value("${xxl.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;


    /**
     * 慢线程池的最大线程数
     */
    @Value("${xxl.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;


    // 下面的是DAO-----------------------------------
    @Resource
    @Getter
    private XxlJobLogDao xxlJobLogDao;
    @Resource
    @Getter
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    @Getter
    private XxlJobRegistryDao xxlJobRegistryDao;
    @Resource
    @Getter
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    @Getter
    private XxlJobLogReportDao xxlJobLogReportDao;
    @Resource
    @Getter
    private JavaMailSender mailSender;
    @Resource
    @Getter
    private DataSource dataSource;

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }


    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
