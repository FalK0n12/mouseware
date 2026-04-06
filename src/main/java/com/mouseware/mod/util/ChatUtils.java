package com.mouseware.mod.util;

import com.mouseware.mod.MacroConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtils {

    private static long lastMessageTime = 0;
    private static final long COOLDOWN_MS = 250;

    public static void send(Minecraft client, String message) {
        if (client == null || client.player == null) return;

        // Keep the sleep on the current thread (the worker thread)
        long now = System.currentTimeMillis();
        long diff = now - lastMessageTime;
        if (diff < COOLDOWN_MS) {
            try { Thread.sleep(COOLDOWN_MS - diff); } catch (InterruptedException ignored) {}
        }

        // Offload the actual network packet sending to the main thread
        client.execute(() -> {
            if (client.player == null || client.getConnection() == null) return;
            if (message.startsWith("/")) {
                client.player.connection.sendCommand(message.substring(1));
            } else {
                client.player.connection.sendChat(message);
            }
        });

        lastMessageTime = System.currentTimeMillis();
    }

    public static void print(Minecraft client, String message) {
        if (client == null) return;
        // Wrapping this in execute prevents the "RenderSystem called from wrong thread" error
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("§9[Mouseware] §r" + message), false);
            }
        });
    }

    public static void debug(Minecraft client, String message) {
        if (!MacroConfig.debug || client == null) return;
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("§9[Mouseware] §8[Debug] §7" + message), false);
            }
        });
    }
}