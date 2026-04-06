package com.mouseware.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.*;

public class MacroConfig {

    public static boolean striderFishingEnabled = false;
    public static boolean soulWhipSwap = false;
    public static boolean abilitySwap = false;
    public static int swapDelay = 50;
    public static int striderThreshold = 27;
    public static boolean debug = false;

    // Saved as GLFW key codes, defaulting to O and K
    public static int menuKey  = GLFW.GLFW_KEY_O;
    public static int macroKey = GLFW.GLFW_KEY_K;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("mouseware.json");

    private static class Data {
        boolean striderFishingEnabled = false;
        boolean soulWhipSwap = false;
        boolean abilitySwap = false;
        int swapDelay = 50;
        int striderThreshold = 27;
        boolean debug = false;
        int menuKey  = GLFW.GLFW_KEY_J;
        int macroKey = GLFW.GLFW_KEY_H;
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
                debug                = d.debug;
                menuKey              = d.menuKey;
                macroKey             = d.macroKey;
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
            d.debug                 = debug;
            d.menuKey               = menuKey;
            d.macroKey              = macroKey;
            GSON.toJson(d, w);
        } catch (IOException e) {
            System.err.println("[Mouseware] Failed to save config: " + e.getMessage());
        }
    }
}
