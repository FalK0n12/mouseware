package com.mouseware.mod.gui;

import com.mouseware.mod.MacroConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;

public class ClickGui extends Screen {

    private static final int PANEL_W  = 220;
    private static final int PANEL_H  = 165;

    private static final int TOGGLE_W = 40;
    private static final int TOGGLE_H = 14;

    private static final int SLIDER_W   = 100;
    private static final int SLIDER_H   = 10;

    // Strider Threshold bounds
    private static final int THRESHOLD_MIN = 1;
    private static final int THRESHOLD_MAX = 30;

    // Swap Delay bounds
    private static final int SWAP_DELAY_MIN = 0;
    private static final int SWAP_DELAY_MAX = 150;

    private int panelX, panelY;

    // layout cursor
    private int currentY;

    private boolean draggingSlider  = false;
    private boolean draggingSlider2 = false;
    private int sliderY;  // striderThreshold slider Y (dynamic)
    private int sliderY2; // swapDelay slider Y (dynamic)

    public ClickGui() {
        super(Component.literal("Mouseware"));
    }

    @Override
    protected void init() {
        panelX = (width  - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        ScreenMouseEvents.allowMouseClick(this).register((scr, event) -> {
            if (event.button() == 0) {

                if (hitToggle(event.x(), event.y(), panelY + 24)) MacroConfig.striderFishingEnabled = !MacroConfig.striderFishingEnabled;
                if (hitToggle(event.x(), event.y(), panelY + 46)) MacroConfig.soulWhipSwap = !MacroConfig.soulWhipSwap;
                if (hitToggle(event.x(), event.y(), panelY + 68)) MacroConfig.abilitySwap = !MacroConfig.abilitySwap;
                if (hitToggle(event.x(), event.y(), sliderY + 28)) MacroConfig.debug = !MacroConfig.debug;

                if (hitSlider(event.x(), event.y(), sliderY)) {
                    draggingSlider = true;
                    updateThresholdFromMouse(event.x());
                }

                if (hitSlider(event.x(), event.y(), sliderY2)) {
                    draggingSlider2 = true;
                    updateSwapDelayFromMouse(event.x());
                }
            }
            return true;
        });

        ScreenMouseEvents.allowMouseRelease(this).register((scr, event) -> {
            if (event.button() == 0) {
                if (draggingSlider || draggingSlider2) {
                    draggingSlider  = false;
                    draggingSlider2 = false;
                    MacroConfig.save();
                }
            }
            return true;
        });

        ScreenKeyboardEvents.allowKeyPress(this).register((scr, event) -> {
            if (event.key() == 256) onClose();
            return true;
        });
    }

    private boolean hitToggle(double mx, double my, int ty) {
        int tx = panelX + PANEL_W - 10 - TOGGLE_W;
        return mx >= tx && mx <= tx + TOGGLE_W && my >= ty && my <= ty + TOGGLE_H;
    }

    private boolean hitSlider(double mx, double my, int sy) {
        int sx = panelX + PANEL_W - 10 - SLIDER_W;
        return mx >= sx && mx <= sx + SLIDER_W && my >= sy && my <= sy + SLIDER_H;
    }

    private void updateThresholdFromMouse(double mx) {
        int sx = panelX + PANEL_W - 10 - SLIDER_W;
        double ratio = Math.max(0, Math.min(1, (mx - sx) / (double) SLIDER_W));
        MacroConfig.striderThreshold = (int) Math.round(THRESHOLD_MIN + ratio * (THRESHOLD_MAX - THRESHOLD_MIN));
    }

    private void updateSwapDelayFromMouse(double mx) {
        int sx = panelX + PANEL_W - 10 - SLIDER_W;
        double ratio = Math.max(0, Math.min(1, (mx - sx) / (double) SLIDER_W));
        MacroConfig.swapDelay = (int) Math.round(SWAP_DELAY_MIN + ratio * (SWAP_DELAY_MAX - SWAP_DELAY_MIN));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0) {
            if (draggingSlider) {
                updateThresholdFromMouse(event.x());
                return true;
            }
            if (draggingSlider2) {
                updateSwapDelayFromMouse(event.x());
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC1A1A2E);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 18, 0xCC16213E);
        g.drawString(font, "§lMouseware", panelX + 6, panelY + 5, 0xFFE0E0E0, false);

        currentY = panelY + 24;

        // 1. Strider Fishing
        renderRow(g, "Strider Fishing", currentY, MacroConfig.striderFishingEnabled);
        currentY += 22;

        // 2. Soul Whip Swap
        renderRow(g, "  Soul Whip Swap", currentY, MacroConfig.soulWhipSwap);
        currentY += 22;

        // 3. Ability Swap
        renderRow(g, "  Ability Swap", currentY, MacroConfig.abilitySwap);
        currentY += 26;

        // 4. Swap Delay slider
        sliderY2 = currentY;
        renderSlider(g, "Swap Delay", sliderY2, MacroConfig.swapDelay, SWAP_DELAY_MIN, SWAP_DELAY_MAX);
        currentY += 22;

        // 5. Strider Threshold slider
        sliderY = currentY;
        renderSlider(g, "Strider Threshold", sliderY, MacroConfig.striderThreshold, THRESHOLD_MIN, THRESHOLD_MAX);
        currentY += 22;

        // 6. Divider
        g.fill(panelX + 4, currentY, panelX + PANEL_W - 4, currentY + 1, 0x55FFFFFF);
        currentY += 6;

        // 7. Debug
        renderRow(g, "Debug", currentY, MacroConfig.debug);

        super.render(g, mx, my, delta);
    }

    private void renderSlider(GuiGraphics g, String label, int sy, int value, int min, int max) {
        int sx = panelX + PANEL_W - 10 - SLIDER_W;

        g.drawString(font, label, panelX + 6, sy + 1, 0xFFE0E0E0, false);

        // Track
        g.fill(sx, sy + 3, sx + SLIDER_W, sy + 7, 0xFF555555);

        // Filled portion
        double ratio = (double)(value - min) / (max - min);
        int filled = (int)(ratio * SLIDER_W);
        g.fill(sx, sy + 3, sx + filled, sy + 7, 0xFF4CAF50);

        // Thumb
        int thumbX = sx + filled;
        g.fill(thumbX - 2, sy, thumbX + 2, sy + SLIDER_H, 0xFFE0E0E0);

        // Value label above thumb
        String val = String.valueOf(value);
        g.drawString(font, val, thumbX - font.width(val) / 2, sy - 9, 0xFFFFFFFF, false);
    }

    private void renderRow(GuiGraphics g, String label, int ty, boolean value) {
        g.drawString(font, label, panelX + 6, ty + 1, 0xFFE0E0E0, false);

        int tx = panelX + PANEL_W - 10 - TOGGLE_W;
        g.fill(tx, ty, tx + TOGGLE_W, ty + TOGGLE_H, value ? 0xFF4CAF50 : 0xFF555555);

        g.drawString(font, value ? "§aON" : "§cOFF", tx + 6, ty + 3, 0xFFFFFFFF, false);
    }

    @Override
    public void onClose() {
        MacroConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}