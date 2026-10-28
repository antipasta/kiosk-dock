// SPDX-License-Identifier: Apache-2.0
package com.github.antipasta.kioskdock;

import me.jxl.kiosk.plugins.OverlaySpec;

/**
 * Screen edge and alignment positions supported by App Dock.
 * All positions on an edge share that edge's anchor so slide animations
 * consistently enter and exit horizontally from side edges and vertically from top/bottom.
 */
enum DockPosition {
    MIDDLE_RIGHT("Middle Right", Edge.RIGHT, Alignment.CENTER),
    UPPER_RIGHT("Upper Right", Edge.RIGHT, Alignment.START),
    LOWER_RIGHT("Lower Right", Edge.RIGHT, Alignment.END),

    MIDDLE_LEFT("Middle Left", Edge.LEFT, Alignment.CENTER),
    UPPER_LEFT("Upper Left", Edge.LEFT, Alignment.START),
    LOWER_LEFT("Lower Left", Edge.LEFT, Alignment.END),

    BOTTOM_CENTER("Bottom Center", Edge.BOTTOM, Alignment.CENTER),
    BOTTOM_LEFT("Bottom Left", Edge.BOTTOM, Alignment.START),
    BOTTOM_RIGHT("Bottom Right", Edge.BOTTOM, Alignment.END),

    TOP_CENTER("Top Center", Edge.TOP, Alignment.CENTER),
    TOP_LEFT("Top Left", Edge.TOP, Alignment.START),
    TOP_RIGHT("Top Right", Edge.TOP, Alignment.END);

    enum Edge {
        RIGHT, LEFT, BOTTOM, TOP
    }

    enum Alignment {
        CENTER, START, END
    }

    private final String title;
    private final Edge edge;
    private final Alignment alignment;

    DockPosition(String title, Edge edge, Alignment alignment) {
        this.title = title;
        this.edge = edge;
        this.alignment = alignment;
    }

    String getTitle() { return title; }
    Edge getEdge() { return edge; }
    Alignment getAlignment() { return alignment; }

    /** The edge anchor determines the slide transition direction. */
    String getAnchor() {
        switch (edge) {
            case LEFT:
                return OverlaySpec.LEFT;
            case BOTTOM:
                return OverlaySpec.BOTTOM;
            case TOP:
                return OverlaySpec.TOP;
            case RIGHT:
            default:
                return OverlaySpec.RIGHT;
        }
    }

    boolean isVertical() {
        return edge == Edge.RIGHT || edge == Edge.LEFT;
    }

    boolean isHorizontal() {
        return edge == Edge.BOTTOM || edge == Edge.TOP;
    }

    int getAlignmentOffset() {
        switch (alignment) {
            case START:
                return -2000;
            case END:
                return 2000;
            case CENTER:
            default:
                return 0;
        }
    }

    static DockPosition fromSetting(Object val) {
        if (val instanceof String) {
            String str = ((String) val).trim();
            for (DockPosition pos : values()) {
                if (pos.title.equalsIgnoreCase(str) || pos.name().equalsIgnoreCase(str)) {
                    return pos;
                }
            }
        }
        return MIDDLE_RIGHT;
    }
}
