package com.ytrue.netty.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-02 9:10
 * @description Netty的Timer接口是一个定时器的抽象，它定义了一组用于调度定时任务的方法。Timer接口的作用主要有以下几点：
 * 1. 定时任务调度：Timer接口提供了方法来调度定时任务，可以在指定的时间间隔后执行任务，或者在指定的时间点执行任务。
 * 2. 异步任务执行：Timer接口可以异步执行定时任务，避免任务的执行阻塞主线程或其他任务的执行。
 * 3. 定时任务取消：Timer接口提供了方法来取消已经调度的定时任务，可以根据任务的唯一标识或其他条件来取消任务的执行。
 * 4. 定时任务管理：Timer接口可以管理已经调度的定时任务，可以获取任务的状态、执行结果等信息，方便对任务进行监控和管理。
 * 5. 定时任务的周期性执行：Timer接口支持周期性执行定时任务，可以重复执行任务，定时触发任务的执行。
 * 6. 定时任务的延迟执行：Timer接口支持延迟执行定时任务，可以在指定的延迟时间后触发任务的执行。
 * 总的来说，Netty的Timer接口提供了一种方便、灵活的定时任务调度机制，可以在网络编程中使用，用于实现定时任务的调度和管理。
 * 通过Timer接口，可以实现任务的延迟执行、周期性执行，以及任务的取消和管理等功能，提供了更加灵活和高效的定时任务处理能力。
 */
public interface Timer {

    /**
     * 创建一个新的定时任务，并在指定的延迟时间后执行。参数 task 是要执行的任务， delay 是延迟时间， unit 是时间单位
     *
     * @param task
     * @param delay
     * @param unit
     * @return
     */
    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);


    /**
     * 停止定时器，取消所有已经调度的任务。已经执行的任务不受影响。
     *
     * @return
     */
    Set<Timeout> stop();
}
