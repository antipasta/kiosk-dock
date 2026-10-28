// SPDX-License-Identifier: Apache-2.0
package com.github.antipasta.kioskdock;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.List;
import me.jxl.kiosk.plugins.KsTheme;
import me.jxl.kiosk.plugins.OverlayFactory;

/**
 * Builds the dock shelf along any screen edge.
 * Features a floating translucent frosted-glass shelf, real APK launcher icons
 * clipped to squircles, tactile press scaling, and adaptive vertical or horizontal layouts.
 */
final class DockViewFactory implements OverlayFactory {
    private final KioskDockPlugin plugin;
    private final List<ShortcutItem> shortcuts;
    private final DockPosition position;
    private final int iconSizeDp;

    DockViewFactory(KioskDockPlugin plugin, List<ShortcutItem> shortcuts, DockPosition position, int iconSizeDp) {
        this.plugin = plugin;
        this.shortcuts = new ArrayList<>(shortcuts);
        this.position = position != null ? position : DockPosition.MIDDLE_RIGHT;
        this.iconSizeDp = iconSizeDp > 0 ? iconSizeDp : 48;
    }

    @Override
    public View create(Context context, KsTheme theme) {
        FrameLayout root = new FrameLayout(context);
        root.setClickable(true);

        final int shelfRadius = theme.px(Math.max(16, (iconSizeDp + 24) / 3));
        root.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), shelfRadius);
            }
        });
        root.setElevation(theme.px(10));

        final boolean isHorizontal = position.isHorizontal();
        final DockPosition.Edge edge = position.getEdge();

        ViewGroup scrollView;
        if (isHorizontal) {
            HorizontalScrollView hsv = new HorizontalScrollView(context);
            hsv.setHorizontalScrollBarEnabled(false);
            hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            hsv.setFillViewport(false);
            scrollView = hsv;
        } else {
            ScrollView vsv = new ScrollView(context);
            vsv.setVerticalScrollBarEnabled(false);
            vsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            vsv.setFillViewport(false);
            scrollView = vsv;
        }

        LinearLayout dockList = new LinearLayout(context);
        dockList.setOrientation(isHorizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        dockList.setGravity(Gravity.CENTER);

        if (isHorizontal) {
            dockList.setPadding(theme.px(14), theme.px(12), theme.px(14), theme.px(12));
        } else {
            dockList.setPadding(theme.px(12), theme.px(14), theme.px(12), theme.px(14));
        }

        final int iconSizePx = theme.px(iconSizeDp);
        final int squircleRadius = (int) Math.max(4, Math.round(iconSizePx * 0.23));
        final float density = context.getResources().getDisplayMetrics().density;
        final float swipeDismissDistance = 25 * density;

        if (shortcuts.isEmpty()) {
            ImageView emptyView = new ImageView(context);
            emptyView.setImageDrawable(icon(theme, "mdi:apps", "onSurfaceVariant"));
            emptyView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            dockList.addView(emptyView, new LinearLayout.LayoutParams(iconSizePx, iconSizePx));
        } else {
            for (int i = 0; i < shortcuts.size(); i++) {
                final ShortcutItem item = shortcuts.get(i);
                final ImageView iconView = new ImageView(context);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconSizePx, iconSizePx);
                if (i < shortcuts.size() - 1) {
                    if (isHorizontal) {
                        lp.rightMargin = theme.px(10);
                    } else {
                        lp.bottomMargin = theme.px(10);
                    }
                }
                iconView.setLayoutParams(lp);
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                // Clip to  squircle shape
                iconView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), squircleRadius);
                    }
                });
                iconView.setClipToOutline(true);

                // Load real app launcher icon from APK with fallback
                iconView.setImageDrawable(loadAppIcon(context, theme, item));

                // Accessibility & tooltip
                iconView.setContentDescription(item.getLabel());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    iconView.setTooltipText(item.getLabel());
                }

                // Tactile press physics + edge-aware swipe-to-dismiss gesture
                iconView.setOnTouchListener(new View.OnTouchListener() {
                    private float startX;
                    private float startY;
                    private long startTime;

                    private boolean checkDismiss(float currentX, float currentY) {
                        float dx = currentX - startX;
                        float dy = currentY - startY;
                        switch (edge) {
                            case LEFT:
                                return -dx > swipeDismissDistance && -dx > Math.abs(dy);
                            case BOTTOM:
                                return dy > swipeDismissDistance && dy > Math.abs(dx);
                            case TOP:
                                return -dy > swipeDismissDistance && -dy > Math.abs(dx);
                            case RIGHT:
                            default:
                                return dx > swipeDismissDistance && dx > Math.abs(dy);
                        }
                    }

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                startX = event.getRawX();
                                startY = event.getRawY();
                                startTime = SystemClock.uptimeMillis();
                                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                float dx = event.getRawX() - startX;
                                float dy = event.getRawY() - startY;
                                if (checkDismiss(event.getRawX(), event.getRawY())) {
                                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                    plugin.hideDock();
                                    return true;
                                }
                                if (Math.abs(dx) > v.getWidth() || Math.abs(dy) > v.getHeight()) {
                                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                                }
                                return true;

                            case MotionEvent.ACTION_UP:
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                                float upDx = Math.abs(event.getRawX() - startX);
                                float upDy = Math.abs(event.getRawY() - startY);
                                long duration = SystemClock.uptimeMillis() - startTime;
                                if (!plugin.isAlwaysShow() && checkDismiss(event.getRawX(), event.getRawY())) {
                                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                    plugin.hideDock();
                                } else if (duration < 600 && upDx < v.getWidth() && upDy < v.getHeight()) {
                                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                    plugin.launchApp(item.getPackageName());
                                }
                                return true;

                            case MotionEvent.ACTION_CANCEL:
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                                return true;
                        }
                        return false;
                    }
                });

                dockList.addView(iconView);
            }
        }

        scrollView.addView(dockList, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        root.addView(scrollView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        onThemeChanged(root, theme);
        return root;
    }

    @Override
    public void onThemeChanged(View view, KsTheme theme) {
        if (!(view instanceof FrameLayout)) return;
        FrameLayout root = (FrameLayout) view;

        // frosted glass shelf styling
        GradientDrawable dockBg = new GradientDrawable();
        dockBg.setShape(GradientDrawable.RECTANGLE);
        dockBg.setCornerRadius(theme.px(22));

        if (theme.isDark()) {
            dockBg.setColor(Color.argb(204, 30, 30, 32)); // ~80% opacity dark glass
            dockBg.setStroke(Math.max(1, theme.px(1)), Color.argb(45, 255, 255, 255)); // Subtle light rim
        } else {
            dockBg.setColor(Color.argb(215, 245, 245, 248)); // ~85% opacity light glass
            dockBg.setStroke(Math.max(1, theme.px(1)), Color.argb(35, 0, 0, 0)); // Subtle dark rim
        }
        root.setBackground(dockBg);
    }

    static Drawable loadAppIcon(Context context, KsTheme theme, ShortcutItem item) {
        if (context != null && item != null && item.getPackageName() != null && !item.getPackageName().trim().isEmpty()) {
            try {
                PackageManager pm = context.getPackageManager();
                if (pm != null) {
                    Drawable appIcon = pm.getApplicationIcon(item.getPackageName());
                    if (appIcon != null) {
                        return appIcon;
                    }
                }
            } catch (Throwable ignored) {
                // Package not found or running in mock unit tests
            }
        }
        // Fallback: theme MDI icon or default
        String iconName = item != null ? item.getIcon() : null;
        if (iconName == null || iconName.trim().isEmpty() || "mdi:apps".equals(iconName)) {
            iconName = "mdi:application";
        }
        Drawable fallback = icon(theme, iconName, "primary");
        if (fallback != null) {
            return fallback;
        }
        return icon(theme, "mdi:apps", "primary");
    }

    private static Drawable icon(KsTheme theme, String name, String role) {
        try {
            return theme.icon(name, role);
        } catch (LinkageError olderHost) {
            return null;
        }
    }
}
