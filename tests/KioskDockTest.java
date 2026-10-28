// SPDX-License-Identifier: Apache-2.0
package com.github.antipasta.kioskdock;

import java.util.*;
import me.jxl.kiosk.plugins.OverlayFactory;
import me.jxl.kiosk.plugins.OverlaySpec;
import me.jxl.kiosk.plugins.PluginHost;

/** Unit tests for KioskDockPlugin lifecycle, handle overlay, commands, positions, and overlay contracts. */
public final class KioskDockTest {
    static final class MockHost implements PluginHost {
        final Map<String, OverlaySpec> shownOverlays = new LinkedHashMap<>();
        final List<String> hiddenOverlays = new ArrayList<>();
        final List<String> loggedMessages = new ArrayList<>();
        final Map<String, String[]> choices = new HashMap<>();
        final Map<String, String[]> choiceLabels = new HashMap<>();
        final List<Map<String, Object>> launchedCommands = new ArrayList<>();
        String lastStatusMessage;
        boolean lastStatusError;

        @Override
        public void showOverlay(String key, OverlaySpec spec, OverlayFactory factory) {
            assert key != null;
            assert spec != null;
            assert factory != null;
            shownOverlays.put(key, spec);
        }

        @Override
        public void hideOverlay(String key) {
            shownOverlays.remove(key);
            hiddenOverlays.add(key);
        }

        @Override
        public void executeCommand(String command, Map<String, Object> arguments, CommandCallback callback) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("command", command);
            entry.put("arguments", arguments);
            launchedCommands.add(entry);

            if ("getLauncherApps".equals(command)) {
                List<Map<String, Object>> mockApps = new ArrayList<>();
                Map<String, Object> app1 = new HashMap<>();
                app1.put("package", "com.portal.calendar");
                app1.put("label", "PortalHub");
                mockApps.add(app1);

                Map<String, Object> app2 = new HashMap<>();
                app2.put("package", "com.spotify.music");
                app2.put("label", "Spotify");
                mockApps.add(app2);

                callback.onResult(true, mockApps, null);
            } else if ("launchApp".equals(command)) {
                callback.onResult(true, true, null);
            } else {
                callback.onResult(false, null, "Unknown command");
            }
        }

        @Override
        public void setSettingChoices(String key, String[] ids, String[] labels) {
            choices.put(key, ids);
            choiceLabels.put(key, labels);
        }

        @Override
        public void status(String message, boolean error) {
            this.lastStatusMessage = message;
            this.lastStatusError = error;
        }

        @Override
        public void log(String message) {
            loggedMessages.add(message);
        }

        @Override public void showWindow(String title, String message, String button) {}
        @Override public void hideWindow() {}
    }

    public static void main(String[] args) {
        MockHost host = new MockHost();
        KioskDockPlugin plugin = new KioskDockPlugin();

        Map<String, Object> settings = new HashMap<>();
        settings.put("position", "Middle Right");
        settings.put("allApps", true);
        settings.put("dockWidth", 80);
        settings.put("showHandle", true);
        settings.put("handleWidth", 16);
        settings.put("handleOffset", -40);

        // 1. Start lifecycle with position = Middle Right, allApps = true
        plugin.start(host, settings);

        assert plugin.isAllApps();
        assert plugin.getPosition() == DockPosition.MIDDLE_RIGHT;
        assert plugin.getDockWidth() == 80;
        assert plugin.isShowHandle();
        assert plugin.getHandleWidth() == 16;
        assert plugin.getHandleOffset() == -40;

        // Verify choices were populated from getLauncherApps (2 launcher apps found)
        assert host.choices.containsKey("shortcuts.package");
        assert host.choices.get("shortcuts.package").length == 2;
        assert plugin.getLauncherApps().size() == 2;
        assert plugin.getActiveShortcuts().size() == 2;

        // Verify peeking edge handle overlay on the right edge
        assert host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY) : "dock handle should be shown initially";
        OverlaySpec handleSpec = host.shownOverlays.get(KioskDockPlugin.HANDLE_KEY);
        assert OverlaySpec.RIGHT.equals(handleSpec.anchor());
        assert handleSpec.width() == 16;
        assert handleSpec.height() == (2 * 48 + 10 + 28); // 134dp matching 2 apps
        assert handleSpec.offsetY() == -40;
        assert handleSpec.insetRight() == 0 : "Handle must be flush (0 inset) against right screen bezel";
        assert handleSpec.insetTop() == 16;
        assert handleSpec.insetBottom() == 16;
        assert !handleSpec.closeOnBack();
        assert !handleSpec.closeOnOutsideTouch();

        // 2. Show dock command
        assert !plugin.isDockVisible();
        plugin.execute("showDock", Collections.emptyMap());
        assert plugin.isDockVisible() : "dock should be visible";
        assert !host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY) : "handle should be hidden while dock is open";
        assert host.shownOverlays.containsKey(KioskDockPlugin.OVERLAY_KEY) : "dock overlay was not shown";

        OverlaySpec dockSpec = host.shownOverlays.get(KioskDockPlugin.OVERLAY_KEY);
        assert OverlaySpec.RIGHT.equals(dockSpec.anchor());
        assert dockSpec.width() == 80;
        assert dockSpec.height() == OverlaySpec.WRAP;
        assert dockSpec.offsetX() == -8;
        assert dockSpec.offsetY() == -40;
        assert dockSpec.insetRight() == 0 : "Dock shelf must have 0 right inset to slide from right bezel";
        assert dockSpec.insetTop() == 16;
        assert dockSpec.insetBottom() == 16;
        assert OverlaySpec.SLIDE.equals(dockSpec.enter());
        assert OverlaySpec.SLIDE.equals(dockSpec.exit());
        assert dockSpec.closeOnOutsideTouch();
        assert dockSpec.closeOnBack();

        // 3. Hide dock command
        plugin.execute("hideDock", Collections.emptyMap());
        assert !plugin.isDockVisible();
        assert !host.shownOverlays.containsKey(KioskDockPlugin.OVERLAY_KEY);
        assert host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY) : "handle should be restored when dock hides";

        // 4. Toggle dock command
        plugin.execute("toggleDock", Collections.emptyMap());
        assert plugin.isDockVisible();
        assert host.shownOverlays.containsKey(KioskDockPlugin.OVERLAY_KEY);

        // 5. Overlay closed event
        plugin.onEvent("overlay.closed", Collections.singletonMap("key", KioskDockPlugin.OVERLAY_KEY));
        assert !plugin.isDockVisible();
        assert host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY);

        // 6. Launch app
        plugin.showDock();
        plugin.launchApp("com.portal.calendar");
        assert !plugin.isDockVisible();
        assert host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY);

        boolean foundLaunch = false;
        for (Map<String, Object> cmd : host.launchedCommands) {
            if ("launchApp".equals(cmd.get("command"))) {
                Map<?, ?> cmdArgs = (Map<?, ?>) cmd.get("arguments");
                if ("com.portal.calendar".equals(cmdArgs.get("package"))) {
                    foundLaunch = true;
                    break;
                }
            }
        }
        assert foundLaunch : "launchApp command was not dispatched";

        // 7. Test Bottom Center position 
        settings.put("position", "Bottom Center");
        settings.put("handleOffset", 20);
        plugin.configure(settings);

        assert plugin.getPosition() == DockPosition.BOTTOM_CENTER;
        assert plugin.getPosition().isHorizontal();

        OverlaySpec bottomHandleSpec = host.shownOverlays.get(KioskDockPlugin.HANDLE_KEY);
        assert OverlaySpec.BOTTOM.equals(bottomHandleSpec.anchor());
        assert bottomHandleSpec.width() == (2 * 48 + 10 + 28); // 134dp horizontal width
        assert bottomHandleSpec.height() == 16; // 16dp pull tab thickness on bottom
        assert bottomHandleSpec.offsetX() == 20;
        assert bottomHandleSpec.offsetY() == 0;
        assert bottomHandleSpec.insetBottom() == 0 : "Bottom handle must have 0 bottom inset to be flush with bottom bezel";
        assert bottomHandleSpec.insetLeft() == 16;
        assert bottomHandleSpec.insetRight() == 16;

        plugin.showDock();
        OverlaySpec bottomDockSpec = host.shownOverlays.get(KioskDockPlugin.OVERLAY_KEY);
        assert OverlaySpec.BOTTOM.equals(bottomDockSpec.anchor());
        assert bottomDockSpec.width() == OverlaySpec.WRAP;
        assert bottomDockSpec.height() == 80;
        assert bottomDockSpec.offsetX() == 20;
        assert bottomDockSpec.offsetY() == -8; // 8dp offset up from bottom edge
        assert bottomDockSpec.insetBottom() == 0;
        plugin.hideDock();

        // 8. Test Upper Left position (Vertical dock on left edge, slides out from left)
        settings.put("position", "Upper Left");
        settings.put("handleOffset", 0);
        plugin.configure(settings);

        assert plugin.getPosition() == DockPosition.UPPER_LEFT;
        assert plugin.getPosition().isVertical();

        OverlaySpec leftHandleSpec = host.shownOverlays.get(KioskDockPlugin.HANDLE_KEY);
        assert OverlaySpec.LEFT.equals(leftHandleSpec.anchor()) : "Upper Left must anchor to LEFT so slide is horizontal";
        assert leftHandleSpec.width() == 16;
        assert leftHandleSpec.height() == (2 * 48 + 10 + 28);
        assert leftHandleSpec.offsetX() == 0;
        assert leftHandleSpec.offsetY() == -2000;
        assert leftHandleSpec.insetLeft() == 0 : "Left handle must have 0 left inset to be flush with left bezel";
        assert leftHandleSpec.insetTop() == 16;
        assert leftHandleSpec.insetBottom() == 16;

        plugin.showDock();
        OverlaySpec leftDockSpec = host.shownOverlays.get(KioskDockPlugin.OVERLAY_KEY);
        assert OverlaySpec.LEFT.equals(leftDockSpec.anchor()) : "Upper Left must anchor to LEFT so slide is horizontal";
        assert leftDockSpec.width() == 80;
        assert leftDockSpec.height() == OverlaySpec.WRAP;
        assert leftDockSpec.offsetX() == 8; // 8dp offset inward from left edge
        assert leftDockSpec.offsetY() == -2000;
        assert leftDockSpec.insetLeft() == 0;
        plugin.hideDock();

        // 9. Test Lower Right position (Vertical dock on right edge, slides out horizontally from right)
        settings.put("position", "Lower Right");
        settings.put("handleOffset", 0);
        plugin.configure(settings);
        assert plugin.getPosition() == DockPosition.LOWER_RIGHT;
        assert OverlaySpec.RIGHT.equals(plugin.getPosition().getAnchor()) : "Lower Right must anchor to RIGHT";

        plugin.showDock();
        OverlaySpec lowerRightDockSpec = host.shownOverlays.get(KioskDockPlugin.OVERLAY_KEY);
        assert OverlaySpec.RIGHT.equals(lowerRightDockSpec.anchor()) : "Lower Right must anchor to RIGHT so it slides out horizontally";
        assert lowerRightDockSpec.offsetY() == 2000;
        plugin.hideDock();

        // 10. Test custom shortcuts mode (allApps = false)
        settings.put("allApps", false);
        List<Map<String, Object>> customShortcuts = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("package", "com.portal.calendar");
        customShortcuts.add(row1);
        settings.put("shortcuts", customShortcuts);
        plugin.configure(settings);

        assert !plugin.isAllApps();
        assert plugin.getActiveShortcuts().size() == 1;
        assert "com.portal.calendar".equals(plugin.getActiveShortcuts().get(0).getPackageName());
        assert "PortalHub".equals(plugin.getActiveShortcuts().get(0).getLabel());

        OverlaySpec customHandleSpec = host.shownOverlays.get(KioskDockPlugin.HANDLE_KEY);
        assert customHandleSpec.height() == (48 + 28); // 76dp matching 1 app

        // 10. Disable handle via configure
        settings.put("showHandle", false);
        plugin.configure(settings);
        assert !plugin.isShowHandle();
        assert !host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY);

        // 11. Test alwaysShow = true mode (permanently pinned dock)
        settings.put("showHandle", true);
        settings.put("alwaysShow", true);
        plugin.configure(settings);

        assert plugin.isAlwaysShow();
        assert plugin.isDockVisible() : "dock should be automatically visible when alwaysShow is true";
        assert !host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY) : "handle must not be shown when alwaysShow is true";
        assert host.shownOverlays.containsKey(KioskDockPlugin.OVERLAY_KEY) : "dock overlay must be visible";

        OverlaySpec alwaysShowSpec = host.shownOverlays.get(KioskDockPlugin.OVERLAY_KEY);
        assert !alwaysShowSpec.closeOnOutsideTouch() : "outside touches must not close dock when alwaysShow is true";
        assert !alwaysShowSpec.closeOnBack() : "back button must not close dock when alwaysShow is true";

        // In alwaysShow mode, launching an app should not dismiss the dock
        plugin.launchApp("com.portal.calendar");
        assert plugin.isDockVisible() : "dock must remain visible after launchApp in alwaysShow mode";
        assert host.shownOverlays.containsKey(KioskDockPlugin.OVERLAY_KEY);
        assert !host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY);

        // In alwaysShow mode, if overlay.closed is received, dock should be re-shown
        plugin.onEvent("overlay.closed", Collections.singletonMap("key", KioskDockPlugin.OVERLAY_KEY));
        assert plugin.isDockVisible() : "dock must be re-shown if overlay is closed while alwaysShow is true";
        assert host.shownOverlays.containsKey(KioskDockPlugin.OVERLAY_KEY);

        // Turn off alwaysShow mode
        settings.put("alwaysShow", false);
        plugin.configure(settings);
        assert !plugin.isAlwaysShow();
        assert !plugin.isDockVisible();
        assert host.shownOverlays.containsKey(KioskDockPlugin.HANDLE_KEY) : "handle should reappear when alwaysShow is turned off";

        // 12. Stop lifecycle
        plugin.stop();
        assert !plugin.isDockVisible();

        System.out.println("KioskDockTest passed successfully.");
    }
}
