package com.mouseware.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class MacroConfig {

    public static boolean striderFishingEnabled = false;
    public static boolean soulWhipSwap = false;
    public static boolean abilitySwap = false;
    public static int swapDelay = 50;
    public static int striderThreshold = 27;
    public static boolean superCraftOnFullInventory = false;
    public static boolean toggleMacro = false;
    public static boolean debug = false;

    public static int menuKey  = GLFW.GLFW_KEY_O;
    public static int macroKey = GLFW.GLFW_KEY_K;
    public static int toggleMacroKey = GLFW.GLFW_KEY_K;

    public static List<String> superCraftItems = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("mouseware.json");

    private static class Data {
        boolean striderFishingEnabled = false;
        boolean soulWhipSwap = false;
        boolean abilitySwap = false;
        int swapDelay = 50;
        int striderThreshold = 27;
        boolean superCraftOnFullInventory = false;
        boolean toggleMacro = false;
        boolean debug = false;
        int menuKey  = GLFW.GLFW_KEY_J;
        int macroKey = GLFW.GLFW_KEY_H;
        int toggleMacroKey = GLFW.GLFW_KEY_K;
        List<String> superCraftItems = new ArrayList<>();
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_FILE)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                striderFishingEnabled = d.striderFishingEnabled;
                soulWhipSwap         = d.soulWhipSwap;
                abilitySwap          = d.abilitySwap;
                swapDelay            = d.swapDelay;
                striderThreshold     = Math.max(1, Math.min(200, d.striderThreshold));
                superCraftOnFullInventory = d.superCraftOnFullInventory;
                toggleMacro          = d.toggleMacro;
                debug                = d.debug;
                menuKey              = d.menuKey;
                macroKey             = d.macroKey;
                toggleMacroKey       = d.toggleMacroKey;
                if (d.superCraftItems != null) superCraftItems = d.superCraftItems;
            }
        } catch (IOException e) {
            System.err.println("[Mouseware] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_FILE)) {
            Data d = new Data();
            d.striderFishingEnabled = striderFishingEnabled;
            d.soulWhipSwap          = soulWhipSwap;
            d.abilitySwap           = abilitySwap;
            d.swapDelay             = swapDelay;
            d.striderThreshold      = striderThreshold;
            d.superCraftOnFullInventory = superCraftOnFullInventory;
            d.toggleMacro           = toggleMacro;
            d.debug                 = debug;
            d.menuKey               = menuKey;
            d.macroKey              = macroKey;
            d.toggleMacroKey        = toggleMacroKey;
            d.superCraftItems       = superCraftItems;
            GSON.toJson(d, w);
        } catch (IOException e) {
            System.err.println("[Mouseware] Failed to save config: " + e.getMessage());
        }
    }
}
