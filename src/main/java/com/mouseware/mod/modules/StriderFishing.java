package com.mouseware.mod.modules;

import org.lwjgl.glfw.GLFW;

import com.mouseware.mod.MacroConfig;
import com.mouseware.mod.util.ChatUtils;
import com.mouseware.mod.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.phys.EntityHitResult;

public class StriderFishing {
    public static boolean running = false;
    public static int striderCount = 0;
    private static boolean tauhaniRunning = false;
    private static boolean killingStriders = false;

    private static int tickCounter = 0;
    private static final int COUNT_INTERVAL = 20; // ~1 second

    public static void start(Minecraft client) {
        if (!MacroConfig.striderFishingEnabled || client.player == null) return;
        running = true;
        tauhaniRunning = false;
        tickCounter = 0;
        striderCount = 0;
        ChatUtils.print(client, "§aStrider fishing enabled");
        ChatUtils.send(client, ".ez-startscript fishing:0");
        tauhaniRunning = true;
        ChatUtils.debug(client, "Taunahi script started");
    }

    public static void stop(Minecraft client) {
        if (!running || client.player == null) return;
        running = false;
        if (tauhaniRunning) {
            ChatUtils.send(client, ".ez-stopscript");
            tauhaniRunning = false;
            ChatUtils.debug(client, "Taunahi script stopped");
            uncastRod(client);
        }
        ChatUtils.print(client, "§cStrider fishing disabled");
    }

    public static void toggle(Minecraft client) {
        if (running) stop(client);
        else start(client);
    }

    private static int findAxeSlot(Minecraft client) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private static int findSoulWhipSlot(Minecraft client) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getItem(i);
            String itemName = stack.getHoverName().getString().toLowerCase();
            if (itemName.contains("soul whip"))
                return i;
        }
        return -1;
    }

    private static void uncastRod(Minecraft client) {
        // Ensuring the world interaction happens on the main thread
        client.execute(() -> {
            if (client.player != null && client.player.fishing != null) {
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            }
        });
    }

    private static void attackLookedAtStrider(Minecraft client) {
        client.execute(() -> {
            if (client.player == null || client.gameMode == null) return;
            if (!(client.hitResult instanceof EntityHitResult hitResult)) return;
            if (!(hitResult.getEntity() instanceof Strider)) return;

            client.gameMode.attack(client.player, hitResult.getEntity());
            client.player.swing(InteractionHand.MAIN_HAND);
        });
    }

    private static void killStriders(Minecraft client) {
        if (killingStriders || !running)
            return;

        killingStriders = true;
        int axeSlot = findAxeSlot(client);

        if (MacroConfig.soulWhipSwap) {
            TimeUtils.run(() -> {
                if (MacroConfig.abilitySwap) {
                    client.execute(() -> {
                        if (axeSlot != -1) {
                            client.player.getInventory().setSelectedSlot(axeSlot);
                        }
                    });
                    TimeUtils.sleep(100);
                    client.execute(() -> {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    });
                }
                TimeUtils.sleep(100);
            });
            
            TimeUtils.run(() -> {
                TimeUtils.sleep(100);
                int soulWhipSlot = findSoulWhipSlot(client);
                ChatUtils.debug(client, "found soul whip slot - "+soulWhipSlot);
                if (soulWhipSlot != -1) {
                    client.execute(() -> {
                        client.player.getInventory().setSelectedSlot(soulWhipSlot);
                        ChatUtils.debug(client, "swapped to soul whip");
                    });
                }
                TimeUtils.sleep(200);
                while (striderCount >= MacroConfig.striderThreshold && running) {
                    ChatUtils.debug(client, "striders > 0, attacking");

                    client.execute(() -> {
                        client.player.getInventory().setSelectedSlot(soulWhipSlot);
                    });
                    ChatUtils.debug(client, "swapped to soul whip");
                    TimeUtils.sleep(100);

                    client.execute(() -> {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    });
                    ChatUtils.debug(client, "used soul whip");
                    TimeUtils.sleep(MacroConfig.swapDelay);

                    client.execute(() -> {
                        if (axeSlot != -1) {
                            client.player.getInventory().setSelectedSlot(axeSlot);
                        }
                    });
                    ChatUtils.debug(client, "swapped to axe");
                    TimeUtils.sleep(800);
                }
                killingStriders = false;
            });
        } else {
            TimeUtils.run(() -> {
                TimeUtils.sleep(100);
                ChatUtils.debug(client, "found axe slot - "+axeSlot);
                if (axeSlot != -1) {
                    client.execute(() -> {
                        client.player.getInventory().setSelectedSlot(axeSlot);
                        ChatUtils.debug(client, "swapped to axe");
                    });
                }
                TimeUtils.sleep(300);
                if (MacroConfig.abilitySwap) {
                    client.execute(() -> {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    });
                }
                TimeUtils.sleep(50);
                while (striderCount > 0 && running) {
                    ChatUtils.debug(client, "in attack loop");
                    attackLookedAtStrider(client);
                    TimeUtils.sleep(100);
                }
                killingStriders = false;
            });
        }
    }

    public static void update(Minecraft client) {
        if (!running || client.player == null || client.level == null) return;

        tickCounter++;
        if (tickCounter < COUNT_INTERVAL) return;
        tickCounter = 0;

        // Counting entities is safe on the Render Thread (where update() is called)
        striderCount = 0;
        for (Entity e : client.level.entitiesForRendering()) {
            if (e instanceof Strider) striderCount++;
        }

        if (striderCount > MacroConfig.striderThreshold && tauhaniRunning) {
            tauhaniRunning = false;
            
            TimeUtils.run(() -> {
                TimeUtils.sleep(300);

                ChatUtils.debug(client, "stopping script");

                ChatUtils.send(client, ".ez-stopscript");

                ChatUtils.debug(client, "script stopped");

                ChatUtils.print(client, "§eStrider threshold reached — killing striders");
            });
            killStriders(client);
        }

        // Re-enable script
        if (striderCount <= 1 && !tauhaniRunning && !killingStriders) {
            ChatUtils.send(client, ".ez-startscript fishing:0");
            tauhaniRunning = true;
            ChatUtils.print(client, "§aStriders killed — script restarted");
        }
    }
}
