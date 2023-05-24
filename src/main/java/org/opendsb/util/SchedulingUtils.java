package org.opendsb.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class SchedulingUtils {
    
    private static ThreadFactory threadFactoryProducer(String qualifier) {
        return (r) -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("OpenDSB-" + qualifier + "[" + thread.getName() + "]");
            thread.setDaemon(true);
            return thread;
        };
    }

    public static ExecutorService buildPool(int numberOFThreads, String qualifier) {
		return Executors.newFixedThreadPool(numberOFThreads, threadFactoryProducer(qualifier));
	}

    public static ScheduledExecutorService buildScheduledPool(int numberOFThreads, String qualifier) {
		return Executors.newScheduledThreadPool(numberOFThreads, threadFactoryProducer(qualifier));
	}
}
