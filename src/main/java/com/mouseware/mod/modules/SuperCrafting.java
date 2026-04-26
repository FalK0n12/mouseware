package com.mouseware.mod.modules;

import com.mouseware.mod.MacroConfig;
import com.mouseware.mod.MacroWorkerThread;
import com.mouseware.mod.MousewareClient;
import com.mouseware.mod.util.ChatUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SuperCrafting {

    public static volatile boolean isCrafting = false;
    private static volatile boolean isCraftingDone = false;
    private static boolean toggleMacroForRun = false;
    private static int currentitemIndex = 0;
    private static int craftingStage = 0;
    private static long lastActionTime = 0;
    private static long guiOpenedAtMs = 0;

    private static final long ACTION_DELAY_MS = 200;
    private static final long GUI_OPEN_DELAY_MS = 250;

    private static List<String> getitems() {
        return MacroConfig.superCraftItems;
    }

    public static void simulateKeyPress(int glfwKey) {
        if (!MacroConfig.toggleMacro) return;
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) return;

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            long window = client.getWindow().handle();
            org.lwjgl.glfw.GLFWKeyCallbackI cb = GLFW.glfwSetKeyCallback(window, null);
            if (cb != null) {
                cb.invoke(window, glfwKey, 0, GLFW.GLFW_PRESS, 0);
                cb.invoke(window, glfwKey, 0, GLFW.GLFW_RELEASE, 0);
                GLFW.glfwSetKeyCallback(window, cb);
            }
        });
    }

    public static void start(Minecraft client) {
        start(client, false);
    }

    public static void start(Minecraft client, boolean toggleMacroForRun) {
        if (isCrafting) return;
        isCrafting = true;
        isCraftingDone = false;
        SuperCrafting.toggleMacroForRun = toggleMacroForRun;
        currentitemIndex = 0;
        craftingStage = 0;
        lastActionTime = 0;
        guiOpenedAtMs = 0;
        ChatUtils.debug(client, "SuperCrafting: Starting for " + getitems().size() + " items.");
        if (SuperCrafting.toggleMacroForRun) {
            simulateKeyPress(MousewareClient.getToggleMacroKey());
        }
        sendRecipeCommand(client);
    }

    public static void stop() {
        isCrafting = false;
        isCraftingDone = false;
        toggleMacroForRun = false;
        currentitemIndex = 0;
        craftingStage = 0;
        lastActionTime = 0;
        guiOpenedAtMs = 0;
    }

    public static boolean isComplete() {
        return isCraftingDone;
    }

    public static void handleGui(Minecraft client, AbstractContainerScreen<?> screen) {
        if (!isCrafting) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < ACTION_DELAY_MS) return;

        List<String> items = getitems();
        if (items.isEmpty()) {
            ChatUtils.print(client, "SuperCrafting: No items configured.");
            stop();
            return;
        }

        String currentItem = items.get(currentitemIndex).toLowerCase();
        String title = screen.getTitle().getString().toLowerCase();

        if (!title.contains(currentItem)) {
            ChatUtils.debug(client, "SuperCrafting: Waiting for GUI - " + currentItem + " (got: " + title + ")");
            guiOpenedAtMs = 0;
            return;
        }

        if (guiOpenedAtMs == 0) guiOpenedAtMs = now;
        if (now - guiOpenedAtMs < GUI_OPEN_DELAY_MS) return;

        if (screen.getMenu().slots.size() < 11) {
            ChatUtils.debug(client, "SuperCrafting: GUI not fully loaded.");
            return;
        }

        switch (craftingStage) {
            case 0 -> {
                ChatUtils.debug(client, "SuperCrafting: Opening supercraft for " + currentItem);
                client.gameMode.handleInventoryMouseClick(
                    screen.getMenu().containerId, 10, 0, ClickType.PICKUP, client.player);
                lastActionTime = now;
                craftingStage = 1;
            }
            case 1 -> {
                ChatUtils.debug(client, "SuperCrafting: Maximising amount for " + currentItem);
                client.gameMode.handleInventoryMouseClick(
                    screen.getMenu().containerId, 32, 0, ClickType.QUICK_MOVE, client.player);
                lastActionTime = now;
                craftingStage = 2;
            }
            case 2 -> {
                ChatUtils.debug(client, "SuperCrafting: Crafting all for " + currentItem);
                client.gameMode.handleInventoryMouseClick(
                    screen.getMenu().containerId, 32, 0, ClickType.PICKUP, client.player);
                lastActionTime = now;
                craftingStage = 3;
            }
            case 3 -> {
                ChatUtils.debug(client, "SuperCrafting: Done with " + currentItem + ". Closing GUI.");
                screen.onClose();
                lastActionTime = now;
                guiOpenedAtMs = 0;
                currentitemIndex++;
                craftingStage = 0;

                if (currentitemIndex >= items.size()) {
                    ChatUtils.print(client, "SuperCrafting: All done!");
                    isCrafting = false;
                    isCraftingDone = true;
                    if (toggleMacroForRun) {
                        simulateKeyPress(MousewareClient.getToggleMacroKey());
                        toggleMacroForRun = false;
                    }
                } else {
                    MacroWorkerThread.getInstance().submit("SuperCrafting-Next", () -> {
                        MacroWorkerThread.sleep(600);
                        sendRecipeCommand(client);
                    });
                }
            }
        }
    }

    private static void sendRecipeCommand(Minecraft client) {
        String item = getitems().get(currentitemIndex);
        ChatUtils.debug(client, "SuperCrafting: Sending /recipe " + item);
        client.execute(() -> ChatUtils.send(client, "/recipe " + item));
    }
}
