// SPDX-License-Identifier: Apache-2.0
package com.github.antipasta.kioskdock;

/** Represents a single shortcut row configured in the dock settings. */
final class ShortcutItem {
    final String label;
    final String icon;
    final String packageName;

    ShortcutItem(String label, String icon, String packageName) {
        this.label = label != null ? label : "";
        this.icon = icon != null && !icon.isEmpty() ? icon : "mdi:apps";
        this.packageName = packageName != null ? packageName : "";
    }

    String getLabel() { return label; }
    String getIcon() { return icon; }
    String getPackageName() { return packageName; }
}
