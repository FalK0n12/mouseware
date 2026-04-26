package com.mouseware.mod.gui;

import com.mouseware.mod.MacroConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClickGui extends Screen {

    private static final int PANEL_W  = 220;
    private static final int PANEL_H  = 231;

    private static final int TOGGLE_W = 40;
    private static final int TOGGLE_H = 14;

    private static final int SLIDER_W   = 100;
    private static final int SLIDER_H   = 10;

    private static final int THRESHOLD_MIN = 1;
    private static final int THRESHOLD_MAX = 30;

    private static final int SWAP_DELAY_MIN = 0;
    private static final int SWAP_DELAY_MAX = 150;

    private int panelX, panelY;
    private int currentY;

    private boolean draggingSlider  = false;
    private boolean draggingSlider2 = false;
    private int sliderY;
    private int sliderY2;
    private int debugY;
    private int listRowY; // supercraft items row Y
    private int superCraftOnFullY;
    private int toggleMacroY;

    // Active sub-panel (list editor)
    private ListSubPanel activeSubPanel = null;

    public ClickGui() {
        super(Component.literal("Mouseware"));
    }

    @Override
    protected void init() {
        panelX = (width  - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        ScreenMouseEvents.allowMouseClick(this).register((scr, event) -> {
            double mx = event.x(), my = event.y();
            if (event.button() == 0) {

                // Sub-panel takes priority
                if (activeSubPanel != null) {
                    if (activeSubPanel.contains((int) mx, (int) my)) {
                        activeSubPanel.mouseClicked((int) mx, (int) my, font);
                    } else {
                        activeSubPanel.commit();
                        activeSubPanel = null;
                        MacroConfig.save();
                    }
                    return true;
                }

                if (hitToggle(mx, my, panelY + 24)) MacroConfig.striderFishingEnabled = !MacroConfig.striderFishingEnabled;
                if (hitToggle(mx, my, panelY + 46)) MacroConfig.soulWhipSwap = !MacroConfig.soulWhipSwap;
                if (hitToggle(mx, my, panelY + 68)) MacroConfig.abilitySwap = !MacroConfig.abilitySwap;
                if (hitToggle(mx, my, superCraftOnFullY)) MacroConfig.superCraftOnFullInventory = !MacroConfig.superCraftOnFullInventory;
                if (hitToggle(mx, my, toggleMacroY)) MacroConfig.toggleMacro = !MacroConfig.toggleMacro;
                if (hitToggle(mx, my, debugY))       MacroConfig.debug = !MacroConfig.debug;

                if (hitSlider(mx, my, sliderY2)) {
                    draggingSlider2 = true;
                    updateSwapDelayFromMouse(mx);
                }
                if (hitSlider(mx, my, sliderY)) {
                    draggingSlider = true;
                    updateThresholdFromMouse(mx);
                }

                // Supercraft items list row
                if (hitListRow(mx, my)) {
                    activeSubPanel = new ListSubPanel(
                        (int) mx, (int) my, width, height,
                        "Supercraft Items",
                        MacroConfig.superCraftItems,
                        items -> { MacroConfig.superCraftItems = new ArrayList<>(items); MacroConfig.save(); }
                    );
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
            if (activeSubPanel != null) {
                if (event.key() == 256) { activeSubPanel.commit(); activeSubPanel = null; MacroConfig.save(); }
                else activeSubPanel.keyPressed(event.key(), event.scancode(), event.modifiers());
                return true;
            }
            if (event.key() == 256) onClose();
            return true;
        });
    }

    // Forward charTyped to sub-panel
    public boolean charTyped(char c, int mods) {
        if (activeSubPanel != null) { activeSubPanel.charTyped(c, mods); return true; }
        return true;
    }

    private boolean hitToggle(double mx, double my, int ty) {
        int tx = panelX + PANEL_W - 10 - TOGGLE_W;
        return mx >= tx && mx <= tx + TOGGLE_W && my >= ty && my <= ty + TOGGLE_H;
    }

    private boolean hitSlider(double mx, double my, int sy) {
        int sx = panelX + PANEL_W - 10 - SLIDER_W;
        return mx >= sx && mx <= sx + SLIDER_W && my >= sy && my <= sy + SLIDER_H;
    }

    private boolean hitListRow(double mx, double my) {
        return mx >= panelX + 4 && mx <= panelX + PANEL_W - 4
            && my >= listRowY && my <= listRowY + TOGGLE_H + 2;
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

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0) {
            if (draggingSlider)  { updateThresholdFromMouse(event.x()); return true; }
            if (draggingSlider2) { updateSwapDelayFromMouse(event.x()); return true; }
        }
        return false;
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

        // 4. Swap Delay
        sliderY2 = currentY;
        renderSlider(g, "Swap Delay", sliderY2, MacroConfig.swapDelay, SWAP_DELAY_MIN, SWAP_DELAY_MAX);
        currentY += 22;

        // 5. Strider Threshold
        sliderY = currentY;
        renderSlider(g, "Strider Threshold", sliderY, MacroConfig.striderThreshold, THRESHOLD_MIN, THRESHOLD_MAX);
        currentY += 22;

        // 6. Supercraft Items list row
        listRowY = currentY;
        renderListRow(g, "Supercraft Items", listRowY, MacroConfig.superCraftItems, mx, my);
        currentY += 22;

        // 7. Supercraft on Full Inventory
        superCraftOnFullY = currentY;
        renderRow(g, "  SC on Full Inv", superCraftOnFullY, MacroConfig.superCraftOnFullInventory);
        currentY += 22;

        // 8. Toggle external macro during SuperCrafting
        toggleMacroY = currentY;
        renderRow(g, "  Toggle Macro", toggleMacroY, MacroConfig.toggleMacro);
        currentY += 22;

        // 9. Divider
        g.fill(panelX + 4, currentY, panelX + PANEL_W - 4, currentY + 1, 0x55FFFFFF);
        currentY += 6;

        // 10. Debug
        debugY = currentY;
        renderRow(g, "Debug", debugY, MacroConfig.debug);

        super.render(g, mx, my, delta);

        // Sub-panel renders on top
        if (activeSubPanel != null) activeSubPanel.render(g, mx, my, font);
    }

    private void renderSlider(GuiGraphics g, String label, int sy, int value, int min, int max) {
        int sx = panelX + PANEL_W - 10 - SLIDER_W;
        g.drawString(font, label, panelX + 6, sy + 1, 0xFFE0E0E0, false);
        g.fill(sx, sy + 3, sx + SLIDER_W, sy + 7, 0xFF555555);
        double ratio = (double)(value - min) / (max - min);
        int filled = (int)(ratio * SLIDER_W);
        g.fill(sx, sy + 3, sx + filled, sy + 7, 0xFF4CAF50);
        int thumbX = sx + filled;
        g.fill(thumbX - 2, sy, thumbX + 2, sy + SLIDER_H, 0xFFE0E0E0);
        String val = String.valueOf(value);
        g.drawString(font, val, thumbX - font.width(val) / 2, sy - 9, 0xFFFFFFFF, false);
    }

    private void renderRow(GuiGraphics g, String label, int ty, boolean value) {
        g.drawString(font, label, panelX + 6, ty + 1, 0xFFE0E0E0, false);
        int tx = panelX + PANEL_W - 10 - TOGGLE_W;
        g.fill(tx, ty, tx + TOGGLE_W, ty + TOGGLE_H, value ? 0xFF4CAF50 : 0xFF555555);
        g.drawString(font, value ? "§aON" : "§cOFF", tx + 6, ty + 3, 0xFFFFFFFF, false);
    }

    private void renderListRow(GuiGraphics g, String label, int ty, List<String> items, int mx, int my) {
        g.drawString(font, label, panelX + 6, ty + 1, 0xFFE0E0E0, false);

        // Summary badge on the right
        String summary;
        if (items == null || items.isEmpty()) summary = "(empty)";
        else if (items.size() == 1)           summary = items.get(0);
        else                                  summary = items.get(0) + " +" + (items.size() - 1);
        if (font.width(summary) > 70) summary = summary.substring(0, 7) + "..";

        int bx = panelX + PANEL_W - 10 - TOGGLE_W;
        boolean hov = hitListRow(mx, my);
        g.fill(bx, ty, bx + TOGGLE_W, ty + TOGGLE_H, hov ? 0xFF3A5F3A : 0xFF2A3A2A);
        g.drawString(font, summary, bx + 3, ty + 3, 0xFFB0D0B0, false);
    }

    @Override
    public void onClose() {
        if (activeSubPanel != null) { activeSubPanel.commit(); activeSubPanel = null; }
        MacroConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── List sub-panel ────────────────────────────────────────────────────────

    static class ListSubPanel {
        private static final int W         = 240;
        private static final int HDR_H     = 16;
        private static final int PAD       = 5;
        private static final int EDIT_H    = 110; // text area height
        private static final int BTN_H     = 14;
        private static final int TOTAL_H   = HDR_H + PAD + EDIT_H + PAD + BTN_H + PAD;

        private final String label;
        private final java.util.function.Consumer<List<String>> setter;

        // Text area content: one item per line
        private final StringBuilder value;
        private int cursorIndex;
        private int selStart = -1, selEnd = -1;
        private int scrollLine = 0;
        private boolean cursorVisible = true;
        private long lastBlink = System.currentTimeMillis();

        private final int screenW, screenH;
        private int panelX, panelY;

        ListSubPanel(int mx, int my, int sw, int sh, String label, List<String> initial,
                     java.util.function.Consumer<List<String>> setter) {
            this.label   = label;
            this.setter  = setter;
            this.screenW = sw;
            this.screenH = sh;
            String joined = (initial == null || initial.isEmpty()) ? "" : String.join("\n", initial);
            this.value = new StringBuilder(joined);
            this.cursorIndex = value.length();
            this.panelX = Math.max(4, Math.min(mx - W / 2, sw - W - 4));
            this.panelY = Math.max(4, Math.min(my - TOTAL_H / 2, sh - TOTAL_H - 4));
        }

        // ── Geometry helpers ─────────────────────────────────────────────────

        private int textLeft()   { return panelX + PAD + 2; }
        private int textTop()    { return panelY + HDR_H + PAD; }
        private int textRight()  { return panelX + W - PAD - 2; }
        private int textBottom() { return textTop() + EDIT_H; }

        private int lineStep(net.minecraft.client.gui.Font font) {
            return font.lineHeight + 2;
        }

        private int visibleLines(net.minecraft.client.gui.Font font) {
            return Math.max(1, EDIT_H / lineStep(font));
        }

        private List<String> lines() {
            return Arrays.asList(value.toString().split("\n", -1));
        }

        private int maxScroll(net.minecraft.client.gui.Font font) {
            return Math.max(0, lines().size() - visibleLines(font));
        }

        private int cursorLine() {
            int line = 0;
            for (int i = 0; i < Math.min(cursorIndex, value.length()); i++)
                if (value.charAt(i) == '\n') line++;
            return line;
        }

        private int cursorColumn() {
            int col = 0;
            for (int i = Math.min(cursorIndex, value.length()) - 1; i >= 0; i--) {
                if (value.charAt(i) == '\n') break;
                col++;
            }
            return col;
        }

        private int lineStartIndex(int targetLine) {
            int line = 0;
            for (int i = 0; i < value.length(); i++) {
                if (line == targetLine) return i;
                if (value.charAt(i) == '\n') line++;
            }
            return value.length();
        }

        private int lineEndIndex(int startIndex) {
            int idx = value.indexOf("\n", startIndex);
            return idx == -1 ? value.length() : idx;
        }

        private void ensureCursorVisible(net.minecraft.client.gui.Font font) {
            int cl = cursorLine(), vis = visibleLines(font);
            if (cl < scrollLine)           scrollLine = cl;
            else if (cl >= scrollLine + vis) scrollLine = cl - vis + 1;
            scrollLine = Math.max(0, Math.min(maxScroll(font), scrollLine));
        }

        // ── Selection helpers ────────────────────────────────────────────────

        private boolean hasSelection() {
            return selStart >= 0 && selEnd >= 0 && selStart != selEnd;
        }
        private int selMin() { return Math.min(selStart, selEnd); }
        private int selMax() { return Math.max(selStart, selEnd); }
        private void clearSel() { selStart = -1; selEnd = -1; }

        private void deleteSelection() {
            if (!hasSelection()) return;
            int lo = selMin(), hi = selMax();
            value.delete(lo, hi);
            cursorIndex = lo;
            clearSel();
        }

        private void insert(String text) {
            if (text == null || text.isEmpty()) return;
            if (hasSelection()) deleteSelection();
            value.insert(cursorIndex, text);
            cursorIndex += text.length();
        }

        private void moveCursorH(int delta) {
            cursorIndex = Math.max(0, Math.min(value.length(), cursorIndex + delta));
        }

        private void moveCursorV(int delta) {
            int tl = Math.max(0, cursorLine() + delta);
            int tc = cursorColumn();
            int ts = lineStartIndex(tl);
            cursorIndex = Math.min(ts + tc, lineEndIndex(ts));
        }

        // ── Render ───────────────────────────────────────────────────────────

        void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            // Border
            g.fill(panelX - 2, panelY - 2, panelX + W + 2, panelY + TOTAL_H + 2, 0xFF4CAF50);
            // Background
            g.fill(panelX, panelY, panelX + W, panelY + TOTAL_H, 0xEE1A1A2E);
            // Header
            g.fill(panelX, panelY, panelX + W, panelY + HDR_H, 0xFF16213E);
            g.drawString(font, "§l" + label, panelX + 4, panelY + 4, 0xFFE0E0E0, false);
            String hint = "(one item per line)";
            g.drawString(font, hint, panelX + W - font.width(hint) - 4, panelY + 4, 0xFF666688, false);

            // Text area background
            g.fill(textLeft() - 2, textTop() - 1, textRight() + 2, textBottom() + 1, 0xFF0D0D1A);

            // Blink cursor
            if (System.currentTimeMillis() - lastBlink > 500) {
                cursorVisible = !cursorVisible;
                lastBlink = System.currentTimeMillis();
            }

            // Clip text area
            g.enableScissor(textLeft() - 1, textTop(), textRight() + 1, textBottom());

            List<String> lineList = lines();
            int step = lineStep(font);
            int visCount = visibleLines(font);
            int caretLine = cursorLine();
            int caretCol  = cursorColumn();

            for (int li = scrollLine; li < lineList.size() && li < scrollLine + visCount; li++) {
                String line = lineList.get(li);
                int drawY = textTop() + (li - scrollLine) * step;

                // Selection highlight
                if (hasSelection()) {
                    int lo = selMin(), hi = selMax();
                    int lineStart = lineStartIndex(li);
                    int lineEnd   = lineEndIndex(lineStart);
                    int slo = Math.max(lo, lineStart);
                    int shi = Math.min(hi, lineEnd);
                    if (slo < shi) {
                        String pre = value.substring(lineStart, slo);
                        String sel = value.substring(slo, shi);
                        int sx0 = textLeft() + font.width(pre);
                        int sx1 = sx0 + font.width(sel);
                        g.fill(Math.max(textLeft(), sx0), drawY,
                               Math.min(textRight(), sx1), drawY + font.lineHeight, 0x664488FF);
                    }
                }

                g.drawString(font, line, textLeft(), drawY, 0xFFE0E0E0, false);

                // Cursor
                if (cursorVisible && li == caretLine && !hasSelection()) {
                    int cx = textLeft() + font.width(line.substring(0, Math.min(caretCol, line.length())));
                    g.fill(cx, drawY - 1, cx + 1, drawY + font.lineHeight, 0xFFFFFFFF);
                }
            }

            g.disableScissor();

            // Scrollbar
            if (maxScroll(font) > 0) {
                int sbX = panelX + W - PAD;
                int sbY0 = textTop(), sbH = EDIT_H;
                int total = lineList.size();
                int thumbH = Math.max(10, sbH * visCount / total);
                int thumbY = sbY0 + (int)((sbH - thumbH) * ((float) scrollLine / maxScroll(font)));
                g.fill(sbX, sbY0, sbX + 2, sbY0 + sbH, 0xFF333355);
                g.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, 0xFF4CAF50);
            }

            // Save button
            int btnY = textBottom() + PAD;
            int btnW = 60;
            int btnX = panelX + (W - btnW) / 2;
            boolean hov = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + BTN_H;
            g.fill(btnX, btnY, btnX + btnW, btnY + BTN_H, hov ? 0xFF4CAF50 : 0xFF2A5A2A);
            String saveLbl = "Save";
            g.drawString(font, saveLbl, btnX + (btnW - font.width(saveLbl)) / 2, btnY + 3, 0xFFFFFFFF, false);
        }

        // ── Input ────────────────────────────────────────────────────────────

        boolean contains(int mx, int my) {
            return mx >= panelX - 2 && mx <= panelX + W + 2
                && my >= panelY - 2 && my <= panelY + TOTAL_H + 2;
        }

        void mouseClicked(int mx, int my, net.minecraft.client.gui.Font font) {
            // Save button
            int btnY = textBottom() + PAD;
            int btnW = 60;
            int btnX = panelX + (W - btnW) / 2;
            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + BTN_H) {
                commit();
                return;
            }

            // Click in text area
            if (mx >= textLeft() - 2 && mx <= textRight() + 2
                    && my >= textTop() && my <= textBottom()) {
                int step = lineStep(font);
                int line = scrollLine + Math.max(0, (my - textTop()) / Math.max(1, step));
                List<String> lineList = lines();
                line = Math.max(0, Math.min(lineList.size() - 1, line));
                String textLine = lineList.get(line);
                int col = 0;
                for (int i = 1; i <= textLine.length(); i++) {
                    if (textLeft() + font.width(textLine.substring(0, i)) > mx) break;
                    col = i;
                }
                int ls = lineStartIndex(line);
                cursorIndex = Math.min(ls + col, ls + textLine.length());
                clearSel();
                ensureCursorVisible(font);
            }
        }

        void keyPressed(int key, int scan, int mods) {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            boolean ctrl  = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
            boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

            if (key == GLFW.GLFW_KEY_A && ctrl) {
                selStart = 0; selEnd = value.length(); cursorIndex = value.length(); return;
            }
            if (key == GLFW.GLFW_KEY_C && ctrl) {
                String toCopy = hasSelection() ? value.substring(selMin(), selMax()) : value.toString();
                Minecraft.getInstance().keyboardHandler.setClipboard(toCopy); return;
            }
            if (key == GLFW.GLFW_KEY_V && ctrl) {
                String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clip != null) { insert(clip.replace("\r", "")); ensureCursorVisible(font); } return;
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (hasSelection()) { deleteSelection(); }
                else if (cursorIndex > 0) { value.deleteCharAt(cursorIndex - 1); cursorIndex--; }
                ensureCursorVisible(font); return;
            }
            if (key == GLFW.GLFW_KEY_DELETE) {
                if (hasSelection()) { deleteSelection(); }
                else if (cursorIndex < value.length()) { value.deleteCharAt(cursorIndex); }
                ensureCursorVisible(font); return;
            }

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (shift) { commit(); return; }
                if (hasSelection()) deleteSelection();
                insert("\n"); ensureCursorVisible(font); return;
            }

            if (key == GLFW.GLFW_KEY_LEFT) {
                int prev = cursorIndex;
                if (shift) { if (!hasSelection()) selStart = prev; moveCursorH(-1); selEnd = cursorIndex; }
                else        { clearSel(); moveCursorH(-1); }
                ensureCursorVisible(font); return;
            }
            if (key == GLFW.GLFW_KEY_RIGHT) {
                int prev = cursorIndex;
                if (shift) { if (!hasSelection()) selStart = prev; moveCursorH(1); selEnd = cursorIndex; }
                else        { clearSel(); moveCursorH(1); }
                ensureCursorVisible(font); return;
            }
            if (key == GLFW.GLFW_KEY_UP) {
                int prev = cursorIndex;
                if (shift) { if (!hasSelection()) selStart = prev; moveCursorV(-1); selEnd = cursorIndex; }
                else        { clearSel(); moveCursorV(-1); }
                ensureCursorVisible(font); return;
            }
            if (key == GLFW.GLFW_KEY_DOWN) {
                int prev = cursorIndex;
                if (shift) { if (!hasSelection()) selStart = prev; moveCursorV(1); selEnd = cursorIndex; }
                else        { clearSel(); moveCursorV(1); }
                ensureCursorVisible(font); return;
            }

            // Fallback character insertion
            if (!ctrl && key == GLFW.GLFW_KEY_SPACE) {
                insert(" ");
                ensureCursorVisible(font);
                return;
            }
            String name = GLFW.glfwGetKeyName(key, scan);
            if (!ctrl && name != null && name.length() == 1) {
                char c = name.charAt(0);
                if (shift) c = Character.toUpperCase(c);
                insert(String.valueOf(c));
                ensureCursorVisible(font);
            }
        }

        void charTyped(char c, int mods) {
            if (c == '\r') return;
            if (hasSelection()) deleteSelection();
            insert(String.valueOf(c));
            ensureCursorVisible(Minecraft.getInstance().font);
        }

        void commit() {
            clearSel();
            List<String> result = new ArrayList<>();
            for (String line : value.toString().split("\\R")) {
                String t = line.trim();
                if (!t.isEmpty()) result.add(t);
            }
            setter.accept(result);
        }
    }
}
