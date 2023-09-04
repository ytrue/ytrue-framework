package com.ytrue.job.core.handler.impl;

import com.ytrue.job.core.context.XxlJobHelper;
import com.ytrue.job.core.handler.IJobHandler;

/**
 * @author ytrue
 * @date 2023-09-04 10:52
 * @description GlueJobHandler
 */
public class GlueJobHandler extends IJobHandler {

    private long glueUpdatetime;
    private IJobHandler jobHandler;

    public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
        this.jobHandler = jobHandler;
        this.glueUpdatetime = glueUpdatetime;
    }
    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }


    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("----------- glue.version:" + glueUpdatetime + " -----------");
        jobHandler.execute();
    }

    @Override
    public void init() throws Exception {
        this.jobHandler.init();
    }

    @Override
    public void destroy() throws Exception {
        this.jobHandler.destroy();
    }
}
