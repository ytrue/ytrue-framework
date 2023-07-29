package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023/7/29 11:35
 * @description 在Netty中， ResourceLeakHint  是用于标识资源泄漏的提示信息。它作为一个字符串常量，用于描述可能发生资源泄漏的上下文或位置。
 * 当Netty检测到资源泄漏时，会生成一个 ResourceLeakHint 对象，并将其与泄漏的资源相关联。这个提示信息可以帮助开发人员更容易地定位和解决资源泄漏问题。
 * ResourceLeakHint  的作用是提供一种方式，使开发人员能够追踪资源泄漏发生的位置。通过在日志或异常信息中包含 ResourceLeakHint ，
 * 开发人员可以根据提示信息找到可能导致资源泄漏的代码位置，从而进行修复或优化。
 * 总而言之， ResourceLeakHint  是一个用于标识资源泄漏位置的提示信息，它在Netty中的作用是帮助开发人员定位和解决资源泄漏问题。
 */
public interface ResourceLeakHint {

    /**
     * Returns a human-readable message that potentially enables easier resource leak tracking.
     *
     * @return
     */
    String toHintString();
}
