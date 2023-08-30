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
import java.util.Arrays;

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


    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            return -1;
        }
        return logretentiondays;
    }


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
        //调用调度器的销毁方法，该方法实际上就是注销之前初始化的一些组件
        //说的直接一点，就是把组件中的线程池资源释放了，让线程池关闭，停止工作
        //当然，这里释放的并不是所有组件的线程资源，可以点进去详细看一下
        xxlJobScheduler.destroy();
    }


    private XxlJobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        //为什么这里可以直接赋值呢？还是和spring的bean对象的初始化有关，XxlJobAdminConfig添加了@Component
        //注解，所以会作为bean被反射创建，创建的时候会调用无参构造器
        //而afterPropertiesSet方法是在容器所有的bean初始化完成式才会被回掉，所以这时候XxlJobAdminConfig对象已经
        //创建完成了，直接赋值this就行
        adminConfig = this;
        //这里就是把调度器对象创建出来
        //在调度器的init方法中，服务端的各个组件就启动了
        xxlJobScheduler = new XxlJobScheduler();
        //初始化服务端的各个组件，在这里，服务端才算真正开始工作了
        xxlJobScheduler.init();
    }
}
