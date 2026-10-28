// SPDX-License-Identifier: Apache-2.0
package com.github.antipasta.kioskdock;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import me.jxl.kiosk.plugins.KsTheme;
import me.jxl.kiosk.plugins.OverlayFactory;

/**
 * Builds the peeking frosted-glass edge of the dock along any screen boundary.
 * Appears as the actual edge of the dock shelf peeking from the screen bezel,
 * adapting its orientation, curvature, and swipe recognition to the chosen edge.
 */
final class HandleViewFactory implements OverlayFactory {
    private final KioskDockPlugin plugin;
    private final DockPosition position;

    HandleViewFactory(KioskDockPlugin plugin, DockPosition position) {
        this.plugin = plugin;
        this.position = position != null ? position : DockPosition.MIDDLE_RIGHT;
    }

    @Override
    public View create(Context context, KsTheme theme) {
        FrameLayout root = new FrameLayout(context);
        root.setClickable(true);

        final int shelfRadius = theme.px(22);
        final int extend = theme.px(10);
        final DockPosition.Edge edge = position.getEdge();

        root.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int w = view.getWidth();
                int h = view.getHeight();
                switch (edge) {
                    case LEFT:
                        outline.setRoundRect(-extend, 0, w, h, shelfRadius);
                        break;
                    case BOTTOM:
                        outline.setRoundRect(0, 0, w, h + extend, shelfRadius);
                        break;
                    case TOP:
                        outline.setRoundRect(0, -extend, w, h, shelfRadius);
                        break;
                    case RIGHT:
                    default:
                        outline.setRoundRect(0, 0, w + extend, h, shelfRadius);
                        break;
                }
            }
        });
        root.setElevation(theme.px(8));

        final float density = context.getResources().getDisplayMetrics().density;
        // Ultra-sensitive swipe threshold (5dp) so bezel sweeps trigger instantly
        final float minSwipeDistance = Math.max(5 * density, 10f);
        final float maxTapSlop = 24 * density;

        root.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;
            private long startTime;
            private boolean triggered;

            private float getInwardDistance(float currentX, float currentY) {
                switch (edge) {
                    case LEFT:
                        return currentX - startX;
                    case BOTTOM:
                        return startY - currentY;
                    case TOP:
                        return currentY - startY;
                    case RIGHT:
                    default:
                        return startX - currentX;
                }
            }

            private float getCrossDistance(float currentX, float currentY) {
                switch (edge) {
                    case LEFT:
                    case RIGHT:
                        return Math.abs(currentY - startY);
                    case BOTTOM:
                    case TOP:
                    default:
                        return Math.abs(currentX - startX);
                }
            }

            private boolean checkSwipe(float currentX, float currentY) {
                float inward = getInwardDistance(currentX, currentY);
                float cross = getCrossDistance(currentX, currentY);
                return inward > minSwipeDistance && inward >= cross * 0.35f;
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        startTime = SystemClock.uptimeMillis();
                        triggered = false;
                        v.setPressed(true);
                        v.setAlpha(0.85f);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (triggered) return true;
                        if (checkSwipe(event.getRawX(), event.getRawY())) {
                            triggered = true;
                            v.setPressed(false);
                            v.setAlpha(1.0f);
                            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                            plugin.showDock();
                            return true;
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        v.setAlpha(1.0f);
                        if (!triggered) {
                            float upDx = Math.abs(event.getRawX() - startX);
                            float upDy = Math.abs(event.getRawY() - startY);
                            long duration = SystemClock.uptimeMillis() - startTime;
                            boolean isSwipe = checkSwipe(event.getRawX(), event.getRawY());
                            boolean isTap = duration < 600 && upDx < maxTapSlop && upDy < maxTapSlop;
                            if (isSwipe || isTap) {
                                triggered = true;
                                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                plugin.showDock();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        v.setAlpha(1.0f);
                        if (!triggered) {
                            float inward = getInwardDistance(event.getRawX(), event.getRawY());
                            float cross = getCrossDistance(event.getRawX(), event.getRawY());
                            // Catch rapid bezel sweeps that crossed the handle window boundary
                            if (inward >= Math.max(3 * density, 6f) && inward >= cross * 0.35f) {
                                triggered = true;
                                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                plugin.showDock();
                            }
                        }
                        triggered = false;
                        return true;
                }
                return false;
            }
        });

        onThemeChanged(root, theme);
        return root;
    }

    @Override
    public void onThemeChanged(View view, KsTheme theme) {
        if (!(view instanceof FrameLayout)) return;
        FrameLayout root = (FrameLayout) view;

        GradientDrawable peekBg = new GradientDrawable();
        peekBg.setShape(GradientDrawable.RECTANGLE);
        float radius = theme.px(22);

        // Curvature matches the screen boundary orientation
        switch (position.getEdge()) {
            case LEFT:
                // Rounded on the right, flush against left screen edge
                peekBg.setCornerRadii(new float[] { 0, 0, radius, radius, radius, radius, 0, 0 });
                break;
            case BOTTOM:
                // Rounded on the top, flush against bottom screen edge
                peekBg.setCornerRadii(new float[] { radius, radius, radius, radius, 0, 0, 0, 0 });
                break;
            case TOP:
                // Rounded on the bottom, flush against top screen edge
                peekBg.setCornerRadii(new float[] { 0, 0, 0, 0, radius, radius, radius, radius });
                break;
            case RIGHT:
            default:
                // Rounded on the left, flush against right screen edge
                peekBg.setCornerRadii(new float[] { radius, radius, 0, 0, 0, 0, radius, radius });
                break;
        }

        if (theme.isDark()) {
            peekBg.setColor(Color.argb(204, 30, 30, 32)); // ~80% opacity dark glass
            peekBg.setStroke(Math.max(1, theme.px(1)), Color.argb(45, 255, 255, 255)); // 1dp hairline rim
        } else {
            peekBg.setColor(Color.argb(215, 245, 245, 248)); // ~85% opacity light glass
            peekBg.setStroke(Math.max(1, theme.px(1)), Color.argb(35, 0, 0, 0)); // 1dp hairline rim
        }
        root.setBackground(peekBg);
    }
}
