// SPDX-License-Identifier: Apache-2.0
package com.github.antipasta.kioskdock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import me.jxl.kiosk.plugins.KioskPlugin;
import me.jxl.kiosk.plugins.OverlaySpec;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Dock plugin for Kiosk Satellite.
 * Displays a floating dock shelf along any edge of the screen.
 * Supports auto-populating all apps from Kiosk Satellite App Launcher,
 * or choosing/reordering specific apps, with a seamless peeking frosted-glass edge handle.
 */
public final class KioskDockPlugin implements KioskPlugin {
    static final String OVERLAY_KEY = "kiosk_dock";
    static final String HANDLE_KEY = "dock_handle";

    private PluginHost host;
    private DockPosition position = DockPosition.MIDDLE_RIGHT;
    private boolean alwaysShow = false;
    private boolean allApps = true;
    private int iconSize = 48;
    private boolean showHandle = true;
    private int handleWidth = 14;
    private int handleOffset = 0;
    private final List<ShortcutItem> launcherApps = new ArrayList<>();
    private final List<ShortcutItem> shortcuts = new ArrayList<>();
    private volatile boolean isDockVisible = false;

    @Override
    public synchronized void start(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        configure(settings);
        refreshAppChoices();
    }

    @Override
    public synchronized void configure(Map<String, Object> settings) {
        position = DockPosition.fromSetting(settings.get("position"));

        Object alwaysShowVal = settings.get("alwaysShow");
        boolean oldAlwaysShow = alwaysShow;
        if (alwaysShowVal instanceof Boolean) {
            alwaysShow = (Boolean) alwaysShowVal;
        }

        Object allAppsVal = settings.get("allApps");
        if (allAppsVal instanceof Boolean) {
            allApps = (Boolean) allAppsVal;
        }

        Object iconVal = settings.get("iconSize");
        if (iconVal instanceof Number) {
            iconSize = Math.max(36, Math.min(72, ((Number) iconVal).intValue()));
        }

        Object handleVal = settings.get("showHandle");
        if (handleVal instanceof Boolean) {
            showHandle = (Boolean) handleVal;
        }

        Object handleWidthVal = settings.get("handleWidth");
        if (handleWidthVal instanceof Number) {
            handleWidth = Math.max(6, Math.min(48, ((Number) handleWidthVal).intValue()));
        }

        Object offsetVal = settings.get("handleOffset");
        if (offsetVal instanceof Number) {
            handleOffset = Math.max(-600, Math.min(600, ((Number) offsetVal).intValue()));
        }

        Object shortcutsVal = settings.get("shortcuts");
        shortcuts.clear();
        if (shortcutsVal instanceof List) {
            for (Object obj : (List<?>) shortcutsVal) {
                if (obj instanceof Map) {
                    Map<?, ?> row = (Map<?, ?>) obj;
                    Object pkg = row.get("package");
                    if (pkg instanceof String && !((String) pkg).trim().isEmpty()) {
                        String pkgStr = ((String) pkg).trim();
                        // Find matching label if available from launcherApps
                        String label = pkgStr;
                        for (ShortcutItem item : launcherApps) {
                            if (pkgStr.equals(item.getPackageName())) {
                                label = item.getLabel();
                                break;
                            }
                        }
                        shortcuts.add(new ShortcutItem(label, "mdi:apps", pkgStr));
                    }
                }
            }
        }

        if (host != null) {
            if (alwaysShow) {
                hideHandle();
                showDock();
            } else if (oldAlwaysShow && !alwaysShow) {
                hideDock();
                if (showHandle) {
                    showHandle();
                }
            } else if (isDockVisible) {
                showDock();
            } else if (showHandle) {
                showHandle();
            } else {
                hideHandle();
            }
        }
    }

    @Override
    public synchronized void execute(String command, Map<String, Object> arguments) {
        if ("toggleDock".equals(command)) {
            toggleDock();
        } else if ("showDock".equals(command)) {
            showDock();
        } else if ("hideDock".equals(command)) {
            hideDock();
        } else {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    @Override
    public synchronized void onEvent(String event, Map<String, Object> payload) {
        if ("settings.choices".equals(event)) {
            refreshAppChoices();
        } else if ("overlay.closed".equals(event) || "ks.overlay.closed".equals(event)) {
            if (payload != null && OVERLAY_KEY.equals(payload.get("key"))) {
                isDockVisible = false;
                if (alwaysShow) {
                    showDock();
                } else if (showHandle) {
                    showHandle();
                }
            }
        }
    }

    @Override
    public synchronized void stop() {
        // Kiosk Satellite revokes host and removes all session overlays before stop().
        this.host = null;
        this.isDockVisible = false;
        this.shortcuts.clear();
        this.launcherApps.clear();
    }

    public synchronized void toggleDock() {
        if (isDockVisible) {
            hideDock();
        } else {
            showDock();
        }
    }

    public synchronized void showDock() {
        if (host == null) return;
        hideHandle();
        try {
            int dockThickness = getDockThickness();
            int width = position.isVertical() ? dockThickness : OverlaySpec.WRAP;
            int height = position.isVertical() ? OverlaySpec.WRAP : dockThickness;
            int alignOffset = position.getAlignmentOffset();
            int offsetX = 0;
            int offsetY = 0;

            switch (position.getEdge()) {
                case LEFT:
                    offsetX = 8;
                    offsetY = alignOffset + handleOffset;
                    break;
                case BOTTOM:
                    offsetX = alignOffset + handleOffset;
                    offsetY = -8;
                    break;
                case TOP:
                    offsetX = alignOffset + handleOffset;
                    offsetY = 8;
                    break;
                case RIGHT:
                default:
                    offsetX = -8;
                    offsetY = alignOffset + handleOffset;
                    break;
            }

            OverlaySpec spec = OverlaySpec.at(position.getAnchor())
                .size(width, height)
                .inset(getInsetLeft(), getInsetTop(), getInsetRight(), getInsetBottom())
                .offset(offsetX, offsetY)
                .enter(OverlaySpec.SLIDE)
                .exit(OverlaySpec.SLIDE)
                .transitionDuration(200)
                .closeOnOutsideTouch(!alwaysShow)
                .closeOnBack(!alwaysShow);

            host.showOverlay(OVERLAY_KEY, spec, new DockViewFactory(this, getActiveShortcuts(), position, iconSize));
            isDockVisible = true;
        } catch (UnsupportedOperationException olderHost) {
            host.status("Native overlays require a newer Kiosk Satellite version.", true);
        }
    }

    public synchronized void hideDock() {
        if (host == null) return;
        try {
            host.hideOverlay(OVERLAY_KEY);
        } catch (Throwable ignored) {}
        isDockVisible = false;
        if (!alwaysShow && showHandle) {
            showHandle();
        }
    }

    public synchronized void showHandle() {
        if (host == null || alwaysShow || !showHandle || isDockVisible) return;
        try {
            int dockLength = calculateDockLength();
            int width = position.isVertical() ? handleWidth : dockLength;
            int height = position.isVertical() ? dockLength : handleWidth;
            int alignOffset = position.getAlignmentOffset();
            int offsetX = position.isVertical() ? 0 : alignOffset + handleOffset;
            int offsetY = position.isVertical() ? alignOffset + handleOffset : 0;

            OverlaySpec spec = OverlaySpec.at(position.getAnchor())
                .size(width, height)
                .inset(getInsetLeft(), getInsetTop(), getInsetRight(), getInsetBottom())
                .offset(offsetX, offsetY)
                .closeOnOutsideTouch(false)
                .closeOnBack(false)
                .touchable(true);

            host.showOverlay(HANDLE_KEY, spec, new HandleViewFactory(this, position));
        } catch (Throwable ignored) {}
    }

    public synchronized void hideHandle() {
        if (host == null) return;
        try {
            host.hideOverlay(HANDLE_KEY);
        } catch (Throwable ignored) {}
    }

    public synchronized void launchApp(String packageName) {
        if (host == null || packageName == null || packageName.trim().isEmpty()) return;
        if (!alwaysShow) {
            hideDock();
        }

        Map<String, Object> args = Collections.singletonMap("package", packageName);
        host.executeCommand("launchApp", args, (ok, data, error) -> {
            if (!ok) {
                if (host != null) {
                    host.status("Failed to launch " + packageName + ": " + error, true);
                    host.log("Dock launchApp error: " + error);
                }
            } else {
                if (host != null) {
                    host.status("Launched " + packageName, false);
                }
            }
        });
    }

    public synchronized List<ShortcutItem> getActiveShortcuts() {
        if (allApps) {
            return new ArrayList<>(launcherApps);
        }
        return new ArrayList<>(shortcuts);
    }

    private int getInsetLeft() {
        return position.isVertical() ? 0 : 16;
    }

    private int getInsetTop() {
        return position.isVertical() ? 16 : 0;
    }

    private int getInsetRight() {
        return position.isVertical() ? 0 : 16;
    }

    private int getInsetBottom() {
        return position.isVertical() ? 16 : 0;
    }

    int calculateDockLength() {
        int count = getActiveShortcuts().size();
        if (count == 0) return iconSize + 28;
        int len = count * iconSize + (count - 1) * 10 + 28;
        return Math.min(800, len);
    }

    @SuppressWarnings("unchecked")
    private void refreshAppChoices() {
        if (host == null) return;
        host.executeCommand("getLauncherApps", Collections.emptyMap(), (ok, data, error) -> {
            if (!ok || !(data instanceof List)) return;
            List<?> list = (List<?>) data;
            List<String> ids = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<ShortcutItem> newLauncherApps = new ArrayList<>();

            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    Object pkg = map.get("package");
                    Object label = map.get("label");
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        String pkgStr = (String) pkg;
                        String labelStr = label instanceof String && !((String) label).isEmpty() ? (String) label : pkgStr;
                        ids.add(pkgStr);
                        labels.add(labelStr);
                        newLauncherApps.add(new ShortcutItem(labelStr, "mdi:apps", pkgStr));
                    }
                }
            }

            synchronized (KioskDockPlugin.this) {
                launcherApps.clear();
                launcherApps.addAll(newLauncherApps);

                // Update shortcut labels if any changed
                for (int i = 0; i < shortcuts.size(); i++) {
                    ShortcutItem sc = shortcuts.get(i);
                    for (ShortcutItem la : launcherApps) {
                        if (sc.getPackageName().equals(la.getPackageName())) {
                            shortcuts.set(i, new ShortcutItem(la.getLabel(), sc.getIcon(), sc.getPackageName()));
                            break;
                        }
                    }
                }

                if (host != null) {
                    try {
                        host.setSettingChoices("shortcuts.package", ids.toArray(new String[0]), labels.toArray(new String[0]));
                    } catch (UnsupportedOperationException | LinkageError ignored) {}

                    if (alwaysShow || isDockVisible) {
                        showDock();
                    } else if (showHandle) {
                        showHandle();
                    }
                }
            }
        });
    }

    // Package-private getters for unit tests
    synchronized boolean isAlwaysShow() { return alwaysShow; }
    synchronized boolean isDockVisible() { return isDockVisible; }
    synchronized boolean isShowHandle() { return showHandle; }
    synchronized boolean isAllApps() { return allApps; }
    synchronized DockPosition getPosition() { return position; }
    synchronized int getHandleWidth() { return handleWidth; }
    synchronized int getHandleOffset() { return handleOffset; }
    synchronized int getIconSize() { return iconSize; }
    synchronized int getDockThickness() { return iconSize + 24; }
    synchronized List<ShortcutItem> getShortcuts() { return new ArrayList<>(shortcuts); }
    synchronized List<ShortcutItem> getLauncherApps() { return new ArrayList<>(launcherApps); }
}
