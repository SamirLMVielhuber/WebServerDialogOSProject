package com.github.dialogos.plugin.remote.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UserTimeoutScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final int TIMEOUT_MINUTES = 5;

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private static final UserTimeoutScheduler INSTANCE = new UserTimeoutScheduler();

    private final Object lock = new Object();

    public static UserTimeoutScheduler getInstance() {
        return INSTANCE;
    }

    public void scheduleStopForUser(String userId, int port) {
        synchronized(lock) {
            if (tasks.containsKey(userId)) return;

            System.out.println("Scheduling user timeout stop for " + userId +
                            " in " + TIMEOUT_MINUTES + " minutes");

            ScheduledFuture<?> f = scheduler.schedule(() -> {
                try {
                    GraphControlListener listener = GraphControlRegistry.getListener();
                    if (listener != null) {
                        System.out.println("User timeout reached, stopping graph for User: " + userId);
                        listener.onStopRequested(userId);
                    }
                    WebSocketHub hub = HubRegistry.getHub(port);
                    if(hub != null){
                        hub.removeUser(userId);
                        if(hub.isEmpty()){
                            System.out.println("No more users in hub for port " + port + ", stopping hub");
                            hub.stop();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, TIMEOUT_MINUTES, TimeUnit.MINUTES);

            tasks.put(userId, f);
        }
    }

    public void cancelStopForUser(String userId) {
        synchronized(lock) {
            ScheduledFuture<?> f = tasks.remove(userId);
            System.out.println("Trying to cancel stop for user " + userId);
            if (f != null) {
                System.out.println("Canceling scheduled user timeout for " + userId);
                f.cancel(false);
            } else {
                System.out.println("No scheduled task found for user " + userId);
            }
        }
    }
}