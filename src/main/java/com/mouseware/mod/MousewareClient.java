package com.mouseware.mod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mouseware.mod.gui.ClickGui;
import com.mouseware.mod.modules.StriderFishing;
import com.mouseware.mod.modules.SuperCrafting;
import com.mouseware.mod.util.TimeUtils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public class MousewareClient implements ClientModInitializer {

    // All printable single-character key names we accept as arguments
    private static final List<String> KEY_SUGGESTIONS = Arrays.asList(
        "a","b","c","d","e","f","g","h","i","j","k","l","m",
        "n","o","p","q","r","s","t","u","v","w","x","y","z",
        "0","1","2","3","4","5","6","7","8","9",
        "f1","f2","f3","f4","f5","f6","f7","f8","f9","f10","f11","f12"
    );

    private static KeyMapping openGuiKey;
    private static KeyMapping startScriptKey;
    private static KeyMapping toggleMacroKey;

    @Override
    public void onInitializeClient() {
        MacroConfig.load();
        TimeUtils.init();
        MacroWorkerThread.getInstance().start();

        ResourceLocation categoryId = ResourceLocation.fromNamespaceAndPath("mouseware", "main");
        KeyMapping.Category category = new KeyMapping.Category(categoryId);

        openGuiKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key/mouseware.config", MacroConfig.menuKey, category));

        startScriptKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key/mouseware.start", MacroConfig.macroKey, category));

        toggleMacroKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key/mouseware.toggle_macro", MacroConfig.toggleMacroKey, category));

        // ── Client-side command registration ─────────────────────────────────
        // The mixin in ChatScreenMixin rewrites /mouseware -> /mouseware on send,
        // so the client dispatcher executes these branches directly.
        // Registering "/mouseware" as a client command gives the chat screen
        // its autocomplete popup just like a real slash command.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            SuggestionProvider<FabricClientCommandSource> keySuggestions =
                (ctx, builder) -> {
                    KEY_SUGGESTIONS.stream()
                        .filter(s -> s.startsWith(builder.getRemaining().toLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                };

            dispatcher.register(
                ClientCommandManager.literal("mouseware")
                    .executes(ctx -> { printUsage(); return 1; })
                    .then(ClientCommandManager.literal("menukey")
                        .then(ClientCommandManager.argument("key", StringArgumentType.word())
                            .suggests(keySuggestions)
                            .executes(ctx -> {
                                // The actual work happens in the chat interceptor above.
                                // This branch only fires if the user runs it as a true
                                // slash-prefixed client command — handle it the same way.
                                handleDotCommand("/mouseware menukey " +
                                    StringArgumentType.getString(ctx, "key"));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("startkey")
                        .then(ClientCommandManager.argument("key", StringArgumentType.word())
                            .suggests(keySuggestions)
                            .executes(ctx -> {
                                handleDotCommand("/mouseware startkey " +
                                    StringArgumentType.getString(ctx, "key"));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("togglemacrokey")
                        .then(ClientCommandManager.argument("key", StringArgumentType.word())
                            .suggests(keySuggestions)
                            .executes(ctx -> {
                                handleDotCommand("/mouseware togglemacrokey " +
                                    StringArgumentType.getString(ctx, "key"));
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("supercraft")
                        .executes(ctx -> {
                            Minecraft client = Minecraft.getInstance();
                            if (MacroConfig.superCraftItems.isEmpty()) {
                                print("§cNo items configured. Add them to §emouseware.json §cunder §esuperCraftItems§c.");
                            } else if (SuperCrafting.isCrafting) {
                                SuperCrafting.stop();
                                print("SuperCrafting stopped.");
                            } else {
                                SuperCrafting.start(client);
                                print("SuperCrafting started for §e" + MacroConfig.superCraftItems.size() + " §ritems.");
                            }
                            return 1;
                        }))
            );
        });

        // ── Chat listener: trigger supercraft on "inventory full" ─────────────
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            StringBuilder sb = new StringBuilder();
            message.visit(text -> { sb.append(text); return java.util.Optional.empty(); });
            String plain = sb.toString().toLowerCase();
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if ((plain.contains("inventory full") || plain.contains("your inventory is full")) && !SuperCrafting.isCrafting && MacroConfig.superCraftOnFullInventory) {
                    if (!MacroConfig.superCraftItems.isEmpty() && client.player != null) {
                        SuperCrafting.start(client, true);
                        print("§eInventory full detected — SuperCrafting started.");
                    }
                }
            });
        });

        // ── Tick handler ──────────────────────────────────────────────────────
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            syncConfiguredKeys();

            while (openGuiKey.consumeClick())
                client.setScreen(new ClickGui());

            while (startScriptKey.consumeClick()) {
                if (MacroConfig.striderFishingEnabled)
                    StriderFishing.toggle(client);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            StriderFishing.update(client);

            // SuperCrafting GUI handler
            if (SuperCrafting.isCrafting && client.screen instanceof AbstractContainerScreen<?> screen) {
                SuperCrafting.handleGui(client, screen);
            }
        });
    }

    // ── Command logic ─────────────────────────────────────────────────────────

    private static void handleDotCommand(String message) {
        // Expected format: "/mouseware menukey <key>", "/mouseware startkey <key>",
        // or "/mouseware togglemacrokey <key>"
        String[] parts = message.split("\\s+");
        if (parts.length < 3) {
            printUsage();
            return;
        }

        String sub = parts[1].toLowerCase();
        String keyArg = parts[2].toLowerCase();

        if (!sub.equals("menukey") && !sub.equals("startkey") && !sub.equals("togglemacrokey") && !sub.equals("togglekey")) {
            printUsage();
            return;
        }

        int glfwKey = resolveKey(keyArg);
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
            print("§cUnknown key: §e" + keyArg + "§c. Use a-z, 0-9, or f1-f12.");
            return;
        }

        if (sub.equals("menukey")) {
            MacroConfig.menuKey = glfwKey;
            openGuiKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(glfwKey));
            KeyMapping.resetMapping();
            MacroConfig.save();
            print("Menu key set to §e" + keyArg.toUpperCase());
        } else if (sub.equals("startkey")) {
            MacroConfig.macroKey = glfwKey;
            startScriptKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(glfwKey));
            KeyMapping.resetMapping();
            MacroConfig.save();
            print("Macro key set to §e" + keyArg.toUpperCase());
        } else {
            MacroConfig.toggleMacroKey = glfwKey;
            toggleMacroKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(glfwKey));
            KeyMapping.resetMapping();
            MacroConfig.save();
            print("Toggle Macro key set to §e" + keyArg.toUpperCase());
        }
    }

    public static int getToggleMacroKey() {
        if (toggleMacroKey == null) return MacroConfig.toggleMacroKey;
        return InputConstants.getKey(toggleMacroKey.saveString()).getValue();
    }

    private static void syncConfiguredKeys() {
        boolean changed = false;
        int menuKey = InputConstants.getKey(openGuiKey.saveString()).getValue();
        int macroKey = InputConstants.getKey(startScriptKey.saveString()).getValue();
        int externalMacroKey = getToggleMacroKey();

        if (MacroConfig.menuKey != menuKey) {
            MacroConfig.menuKey = menuKey;
            changed = true;
        }
        if (MacroConfig.macroKey != macroKey) {
            MacroConfig.macroKey = macroKey;
            changed = true;
        }
        if (MacroConfig.toggleMacroKey != externalMacroKey) {
            MacroConfig.toggleMacroKey = externalMacroKey;
            changed = true;
        }
        if (changed) MacroConfig.save();
    }

    private static void printUsage() {
        print("Usage: §e/mouseware menukey <key> §r, §e/mouseware startkey <key> §ror §e/mouseware togglemacrokey <key>");
    }

    private static void print(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null)
            client.player.displayClientMessage(
                Component.literal("§9[Mouseware] §r" + msg), false);
    }

    /**
     * Maps a string like "j", "f5", "3" to its GLFW key code.
     */
    private static int resolveKey(String name) {
        // Single letter a-z
        if (name.matches("[a-z]")) {
            return GLFW.GLFW_KEY_A + (name.charAt(0) - 'a');
        }
        // Digit 0-9
        if (name.matches("[0-9]")) {
            return GLFW.GLFW_KEY_0 + (name.charAt(0) - '0');
        }
        // Function keys f1-f12
        if (name.matches("f([1-9]|1[0-2])")) {
            int n = Integer.parseInt(name.substring(1));
            return GLFW.GLFW_KEY_F1 + (n - 1);
        }
        return GLFW.GLFW_KEY_UNKNOWN;
    }
}
