package com.mouseware.mod.util;

import com.mouseware.mod.MacroWorkerThread;

/**
 * Helper for sequential macro tasks.
 * Automatically runs submitted code on a background worker thread.
 */
public final class TimeUtils {

    private TimeUtils() {}

    /** Start the worker thread once (call at mod init) */
    public static void init() {
        MacroWorkerThread.getInstance().start();
    }

    /**
     * Runs a block of code on the worker thread sequentially.
     * Example:
     * TimeUtils.run(() -> {
     *     uncastRod(client);
     *     MacroWorkerThread.sleep(300);
     *     client.execute(() -> switchToAxe(client));
     * });
     */
    public static void run(Runnable task) {
        MacroWorkerThread.getInstance().submit("TimeUtils Task", task);
    }

    /** Non-blocking “sleep” inside worker thread */
    public static boolean sleep(long ms) {
        return MacroWorkerThread.sleep(ms);
    }
}