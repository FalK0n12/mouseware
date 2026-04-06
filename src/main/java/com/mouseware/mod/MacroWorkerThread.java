package com.mouseware.mod;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker thread for running sequential macro tasks safely.
 * Allows Thread.sleep() without freezing the game.
 */
public final class MacroWorkerThread {

    private static final MacroWorkerThread INSTANCE = new MacroWorkerThread();

    public static MacroWorkerThread getInstance() {
        return INSTANCE;
    }

    private static final String THREAD_NAME = "mouseware-worker";

    private final LinkedBlockingQueue<TaskEntry> queue = new LinkedBlockingQueue<>();
    private volatile boolean cancelRequested = false;
    private volatile String currentTaskName = "(idle)";
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    private MacroWorkerThread() {}

    public synchronized void start() {
        if (running.get() && workerThread != null && workerThread.isAlive()) return;
        running.set(true);
        workerThread = new Thread(this::loop, THREAD_NAME);
        workerThread.setDaemon(true);
        workerThread.start();
        System.out.println("[Worker] Worker thread started.");
    }

    public void submit(String taskName, Runnable task) {
        queue.add(new TaskEntry(taskName, task));
    }

    public void cancelCurrent() {
        cancelRequested = true;
        queue.clear();
        System.out.println("[Worker] Cancel requested for [" + currentTaskName + "]");
    }

    public boolean isCancelled() {
        return cancelRequested;
    }

    public boolean isBusy() {
        return queue.size() > 0 || !currentTaskName.equals("(idle)");
    }

    public String getCurrentTaskName() {
        return currentTaskName;
    }

    private void loop() {
        while (running.get()) {
            try {
                TaskEntry entry = queue.take(); // blocks until task available
                cancelRequested = false;
                currentTaskName = entry.name;
                try {
                    entry.task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    currentTaskName = "(idle)";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        running.set(false);
    }

    /** Sleep inside a worker task safely */
    public static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Task entry */
    private static final class TaskEntry {
        final String name;
        final Runnable task;
        TaskEntry(String name, Runnable task) {
            this.name = name;
            this.task = task;
        }
    }
}