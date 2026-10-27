# StretchX - Android Resolution & Aspect-Ratio Gaming Utility

StretchX is a specialized Android utility engineered for mobile gamers seeking custom aspect ratios (e.g., stretched 4:3, 16:9, 1:1) and tuned render resolutions with DPI scaling. Leveraging Shizuku's ADB-level capabilities, StretchX applies temporary display adjustments per game, features a lightweight floating gaming bubble, and provides automated safety rollbacks so display settings never get stuck.

***

### User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed during initial planning:
> - **Resolution & Ratio System**: StretchX supports both custom resolution (freeform width, height, density) and preset aspect ratios (e.g., 4:3 Stretched, 16:9 Esports, 1:1 Square, 21:9 Ultrawide).
> - **Floating Game Overlay**: A collapsible floating bubble docked to the screen edge that expands into quick resolution toggles, display stats, and an instant reset switch.
> - **Safety Rollback Engine**: An automated 15-second safety confirmation countdown on manual tests, paired with automatic restoration upon game exit, an Emergency Reset Quick Settings Tile, and a high-priority heads-up notification.

***

## 1. Overview & Core Concept

### What It Does
StretchX empowers competitive and casual Android mobile gamers to adjust display resolutions (`wm size`) and pixel densities (`wm density`) on a per-game profile basis:
- **Aspect Ratio Stretching**: Emulate stretched resolutions (like 4:3 in shooters for wider player models or 1:1 for tactical awareness).
- **Per-Game Library**: Automatically detects installed games and associates customized display profiles.
- **Floating In-Game Bubble**: Quick-access overlay for switching profiles, checking status, or reverting mid-game.
- **Fail-Safe Restoration**: Automatically restores the native resolution and density when games close or if an unconfirmed resolution test times out.

### Technical Boundaries & Shizuku Capabilities
- **Shizuku ADB vs. Root**: StretchX relies on Shizuku's privileged shell execution to call system window manager (`wm`) binaries.
- **Modern Android Limits**: Android 10+ removed `wm overscan`. StretchX implements mathematical viewport scaling (scaling width/height and density proportionally) instead of unsupported overscan hacks.
- **Original State Preservation**: Baseline native resolution and density are captured on first launch and cached safely in persistent storage.

***

## 2. User Experience & Visual Design

### Key User Flows
1. **Shizuku Pairing & Onboarding**:
   - Status card checks Shizuku service availability, ping, and permission status.
   - One-tap button to request Shizuku authorization with clear diagnostic feedback.
2. **Game Library & Profile Setup**:
   - Grid/list of installed games detected on device with category filtering.
   - Profile editor: Choose preset ratio (4:3 Stretched, 16:9, 21:9) or dial in custom Width × Height and DPI.
   - Interactive preview of the target aspect ratio showing calculated pixel density and aspect proportion.
3. **Game Launch & Session**:
   - Launching a game activates its custom display profile via Shizuku shell.
   - A floating bubble appears on-screen; tapping expands quick-actions (FPS/status, resolution switch, instant revert).
4. **Safety & Emergency Recovery**:
   - Testing a resolution triggers a 15-second auto-rollback dialog ("Reverting in 15s... Tap to Keep").
   - A persistent Quick Settings tile and sticky notification allow one-tap emergency reset if the screen becomes unreadable.

### Visual Identity & Theme
- **Theme**: Premium Cyberpunk Dark / Tactical OLED Gaming.
- **Color Palette**:
  - Background: Deep Midnight `#0B0F17` and Charcoal `#131926`
  - Primary Accent: Hyper Cyan `#00F5D4` (energy, active state)
  - Secondary Accent: Neon Coral `#FF3366` (emergency reset, caution)
  - Surface Elevation: Semi-translucent dark slate `#1E2638` with subtle border glows
- **Typography & Motion**: Clean monospace metrics for resolutions (e.g. `1440 × 1080 @ 420 DPI`), fluid spring transitions for the floating bubble, and pulse animations for the safety countdown.

***

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Why | Alternatives Considered |
|---|---|---|---|
| **Privilege Backend** | Shizuku API (`rikka.shizuku:api`) | Standard for unrooted Android power tools; safe, widely adopted, no Magisk/KernelSU root required. | Root-only `su` (excludes majority of users); Accessibility alone (cannot modify `wm size`). |
| **Data Persistence** | Room Database | Structured, reactive queries for game profiles, custom presets, and diagnostic logs with Flow support. | Simple SharedPreferences (too rigid for multi-game profiles and presets). |
| **Overlay Implementation** | Foreground Service + `WindowManager` Window View | Reliable floating bubble overlay across any full-screen game with touch pass-through when collapsed. | In-app dialogs only (cannot be accessed while running third-party games). |
| **Emergency Failsafe** | Triple-layer: 15s Timer + Sticky Notification + Quick Settings Tile | Prevents soft-lock bricking if a user enters an invalid resolution that causes display sync or touch offset issues. | Single in-app button (unusable if touch coordinates skew or screen goes black). |

***

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       StretchX UI Layer                     │
│  ┌─────────────────┐  ┌──────────────────┐  ┌─────────────┐ │
│  │ Dashboard & Lib │  │ Profile Designer │  │ Diagnostics │ │
│  └────────┬────────┘  └────────┬─────────┘  └──────┬──────┘ │
└───────────┼────────────────────┼───────────────────┼────────┘
            ▼                    ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                 StretchX ViewModel & Session                │
│  - Active Session State    - Safety Timer Controller        │
│  - Game Detection Watcher  - Display Configuration State    │
└──────────────────────────┬──────────────────────────────────┘
                           │
            ┌──────────────┴──────────────┐
            ▼                             ▼
┌───────────────────────────┐ ┌───────────────────────────────┐
│       Data Layer          │ │   Shizuku & System Service    │
│  - Room DB (GameProfile,  │ │  - Shizuku Process Manager    │
│    Preset, ConfigHistory) │ │  - DisplayService (`wm size`) │
│  - User Preferences       │ │  - FloatingBubbleService      │
│  - Native Display Backup  │ │  - ResetTileService (QS Tile) │
└───────────────────────────┘ └───────────────────────────────┘
```

### Data Model Entities
- **GameProfile**: `packageName`, `appName`, `iconUri`, `customWidth`, `customHeight`, `customDpi`, `presetRatio`, `autoApplyOnLaunch`, `isFavorite`, `lastUsedTimestamp`.
- **DisplayPreset**: `id`, `name`, `ratioLabel`, `widthRatio`, `heightRatio`, `recommendedDpiMultiplier`, `isDefault`.
- **DisplayState**: `nativeWidth`, `nativeHeight`, `nativeDpi`, `currentWidth`, `currentHeight`, `currentDpi`, `isAltered`.

### Shizuku Command Execution
- Resolution Apply: `wm size <width>x<height>`
- Density Apply: `wm density <dpi>`
- Resolution Reset: `wm size reset`
- Density Reset: `wm density reset`
- Query Native: `wm size` & `wm density` parser

***
