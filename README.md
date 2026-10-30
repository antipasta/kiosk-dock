# Dock

A slide-out application dock for [Kiosk Satellite](https://github.com/jxlarrea/kiosk-satellite).

Launch your favorite Android applications directly from a floating translucent frosted-glass dock shelf without leaving Kiosk Satellite.

## Features

- **Floating Glass Shelf**: Floats slightly off the screen edge with a translucent frosted-glass finish, hairline rim border, and hardware-accelerated soft drop shadow.
- **Peeking Dock Edge**: A subtle frosted edge of the dock shelf peeks from the screen boundary, providing an intuitive, smooth slide-out transition when tapped or swiped.
- **Zero Configuration Out of the Box**: Automatically discovers and displays all applications configured in Kiosk Satellite's App Launcher (`allApps = true`).
- **Real Application Icons**: Automatically retrieves and renders high-resolution launcher icons from installed applications.
- **Squircle Styling**: Clips every app icon to an authentic squircle (`ViewOutlineProvider` with smooth corner curves).
- **Tactile Press Physics**: Micro-scaling press animations (`scale(0.92)` on touch-down, spring back on touch-up) paired with haptic feedback.
- **Minimalist Presentation**: Dedicated purely to your app icons without unnecessary headers or decorative chrome.
- **Auto-Return Compatible**: Seamlessly works with Kiosk Satellite's built-in **Return automatically** feature, bringing the kiosk back after a period of inactivity.
- **Dismiss Anywhere**: Tap outside the dock, press Back, or swipe towards the bezel to dismiss it with a smooth slide transition.
- **Gesture & Drawer Support**: Can also be triggered via Kiosk Satellite's native Gestures (e.g., Corner Tap) or Kiosk Drawer shortcuts.
- **Dark & Light Themes**: Dynamically adapts its frosted glass tone to Kiosk Satellite's Material 3 theme.

## Installation

### Community Plugins (Recommended)

Dock is listed in [Community Plugins](https://kiosksatellite.com/plugins/). Use the button below to open it in your Kiosk Satellite and install it.

[![Install in Kiosk Satellite](https://kiosksatellite.com/img/badges/plugin-add.svg)](https://kiosksatellite.com/redirect/plugin/?repository=antipasta/kiosk-dock)

Or search for `Dock` in **Settings > Plugin Manager > Browse plugins** in Kiosk Satellite.

### Manual

1. Open **Settings > Plugin Manager** in Kiosk Satellite and turn on **Enable Plugins**.
2. Choose **Add plugin**, paste `https://github.com/antipasta/kiosk-dock` and choose **Preview**.
3. Review the plugin details and choose **Trust and install**.
4. Enable **Dock** from its entry row.

## Configuration

### 1. Enable Apps in Kiosk Satellite
Kiosk Satellite enforces a kiosk security boundary so that only approved apps can be launched:
1. Open **Settings > App Launcher > Apps** in Kiosk Satellite and add the apps you want to launch (such as *Home Assistant*, *Spotify*, etc.).
2. Set **Return automatically** (e.g., 30 seconds) so Kiosk Satellite automatically returns to the front when you finish using an app.

### 2. Configure the Dock
1. By default, **All Kiosk Apps** is turned ON: every app you enable in **Settings > App Launcher > Apps** appears in the dock automatically with its real icon.
2. Choose **Dock Position**: Place the dock anywhere you like:
   - **Right Edge**: *Middle Right* (default), *Upper Right*, or *Lower Right*
   - **Left Edge**: *Middle Left*, *Upper Left*, or *Lower Left*
   - **Bottom Edge**: *Bottom Center*, *Bottom Left*, or *Bottom Right*
   - **Top Edge**: *Top Center*, *Top Left*, or *Top Right*
3. **Always Show Dock**: Turn this ON to pin the dock permanently on your screen. Touches to the dashboard pass through, Back does not close it, and the pull tab is hidden.
4. If you want a custom subset or specific ordering:
   - Open **Settings > Plugin Manager > Dock**.
   - Turn off **All Kiosk Apps**.
   - Tap **Add app** under **Custom App Selection** and select an app from the list.
5. (Optional) Adjust **Pull Tab Width** (how many pixels peek out), **Position Offset** (fine-tuning along the edge), or **Icon Size**.

### 3. Open the Dock
- **Via Peeking Edge**: Tap or swipe inward from the bezel directly on the peeking frosted edge of the dock.
- **Via Gestures**: In **Settings > Gestures**, map a corner tap to **Run a plugin action > Dock: Toggle Dock**.
- **Via Drawer**: In **Settings > Plugin Manager > Dock**, tap **Toggle Dock** and turn on **Show in kiosk drawer**.

## Settings Reference

| Setting | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `position` | Select | `Middle Right` | Screen edge and alignment (12 positions across Right, Left, Bottom, Top). |
| `alwaysShow` | Boolean | `false` | Keep the dock permanently visible on screen (pinned desktop dock). |
| `allApps` | Boolean | `true` | Automatically show all apps configured in Kiosk Satellite App Launcher. |
| `shortcuts` | List | `[]` | Custom app selection when `allApps` is off. Pick specific apps and drag to reorder. |
| `showHandle` | Boolean | `true` | Show the frosted edge of the dock peeking from the screen edge (when not in always-show mode). |
| `handleWidth` | Number | `14` | How many pixels (dp) of the dock peek out from the screen edge as a pull tab (6 to 48 dp). |
| `handleOffset`| Number | `0` | Fine-tune dock position along its edge (offset from center, -400 to 400 dp). |
| `iconSize` | Number | `48` | Size of app icons in dp (36 to 72 dp). The dock shelf automatically scales to fit. |

## Actions & Commands

- **Toggle Dock** (`toggleDock`): Opens the dock if closed, or closes it if open.
- **Show Dock** (`showDock`): Slides the dock into view.
- **Hide Dock** (`hideDock`): Slides the dock out to dismiss it.

## Privacy & Local Data

Dock runs entirely on your device inside Kiosk Satellite.
- **No Network Tracking**: Dock makes zero external network requests.
- **No Telemetry**: No analytics or usage data are collected.
- **Local Storage**: All shortcut names and package preferences remain in Kiosk Satellite's local settings storage.

## Requirements

- Kiosk Satellite `2026.10.42` or newer.
- Android 7.0 (API 24) or newer.

## License

[Apache-2.0](LICENSE).
