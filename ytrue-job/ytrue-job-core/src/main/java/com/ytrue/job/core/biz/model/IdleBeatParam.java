package com.ytrue.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-08-31 9:59
 * @description IdleBeatParam
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IdleBeatParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private int jobId;
}
