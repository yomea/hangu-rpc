package com.hangu.common.manager;

import com.hangu.common.constant.hanguCons;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wuzhenhong
 * @date 2024/3/7 9:52
 */
public class HanguExecutorManager {

    private static final Object EXECUTOR_LOCK = new Object();
    private static final Object SCHEDULE_LOCK = new Object();

    private static volatile ExecutorService GLOBAL_EXECUTOR;
    private static volatile ScheduledExecutorService SCHEDULE_EXECUTOR;

    public static final void openAllGlobalExecutor(int coreNum, int maxNum) {
        openIoExecutor(coreNum, maxNum);
        openScheduledExecutor();
    }

    public static final Executor openIoExecutor(int coreNum, int maxNum) {
        if (Objects.nonNull(GLOBAL_EXECUTOR)) {
            return GLOBAL_EXECUTOR;
        }
        synchronized (EXECUTOR_LOCK) {
            if (Objects.nonNull(GLOBAL_EXECUTOR)) {
                return GLOBAL_EXECUTOR;
            }
            coreNum = coreNum <= 0 ? hanguCons.DEF_IO_THREADS * 4 : coreNum;
            maxNum = maxNum <= 0 ? hanguCons.CPUS * 8 : maxNum;

            ExecutorService executor = new ThreadPoolExecutor(coreNum, maxNum,
                10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10000));

            GLOBAL_EXECUTOR = executor;
        }
        return GLOBAL_EXECUTOR;
    }

    public static final Executor openScheduledExecutor() {
        if (Objects.nonNull(SCHEDULE_EXECUTOR)) {
            return SCHEDULE_EXECUTOR;
        }
        synchronized (SCHEDULE_LOCK) {
            if (Objects.nonNull(SCHEDULE_EXECUTOR)) {
                return SCHEDULE_EXECUTOR;
            }
            ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(hanguCons.CPUS);

            SCHEDULE_EXECUTOR = executor;
        }
        return SCHEDULE_EXECUTOR;
    }


    public static final Executor getGlobalExecutor() {
        return GLOBAL_EXECUTOR;
    }

    public static final ScheduledExecutorService getSchedule() {
        return SCHEDULE_EXECUTOR;
    }

    public static final void close() {
        if (Objects.nonNull(GLOBAL_EXECUTOR) && !GLOBAL_EXECUTOR.isShutdown()) {
            GLOBAL_EXECUTOR.shutdown();
        }
        if (Objects.nonNull(SCHEDULE_EXECUTOR) && !SCHEDULE_EXECUTOR.isShutdown()) {
            SCHEDULE_EXECUTOR.shutdown();
        }
    }
}
