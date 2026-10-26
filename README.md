<div align="center">

# Full GrimAC

**An advanced, high-performance simulation anticheat fork for Paper, Purpur, Spigot, and Folia with enhanced combat, inventory, and dynamic performance protections.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.8--1.21.11+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-17+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-1.3-2b2b2b.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-GPL--3.0-2b2b2b.svg?style=flat-square)](LICENSE)

[Overview](#overview) • [Fork Notice](#fork-notice) • [Key Features](#key-features) • [Compatibility](#compatibility) • [Commands and Permissions](#commands-and-permissions) • [Configuration](#configuration) • [Localization](#localization) • [Installation](#installation) • [Compiling from Source](#compiling-from-source) • [License & Attribution](#license--attribution)

</div>

---

## Overview

Full GrimAC is an open-source, modern simulation-based anticheat designed to provide rigorous cheat prevention across Minecraft versions 1.8 through 1.21.11+. By running asynchronous movement simulation on Netty network threads, Full GrimAC reconstructs player movement, world collisions, and player physics with mathematical precision, eliminating movement false positives while remaining impossible to bypass.

Building upon the robust foundation of the upstream GrimAC project, Full GrimAC introduces specialized packet-level detection modules, including advanced combat autoclicker analysis, instant totem and inventory swap exploit detection, autonomous phantom aura verification bots, and an adaptive performance protection system designed for heavy server loads.

---

## Fork Notice

> [!IMPORTANT]
> **Full GrimAC is an independent open-source fork of [GrimAC](https://github.com/GrimAnticheat/Grim), originally developed by the GrimAC Team.**
> - This project is distributed under the terms of the **GNU General Public License v3.0 (GNU GPLv3)**.
> - Full GrimAC is **not** an official release of the GrimAC project or the upstream GrimAC development team.
> - Modifications, enhancements, and custom modules are developed and maintained by **NothingFlare**.
> - Inquiries, bug reports, and pull requests relating to this fork should be submitted directly to this repository.

---

## Key Features

- **1:1 Physics & Movement Simulation Engine (Inherited from GrimAC)**
  - Recreates the player's movement on the Netty thread, covering walking, swimming, knockback, cobwebs, bubble columns, and vehicle riding (boats, horses, striders).
  - Maintains a per-player asynchronous world replica using palette-compressed chunk caching.
  - Complete latency and inventory compensation prevents ghost blocks and desynchronization under high latency.
  - Full support for ViaVersion, ViaBackwards, and Geyser/Floodgate bedrock exemption.

- **Advanced Combat & Click Consistency Detection (Full GrimAC Additions)**
  - `CombatAutoClicker`: Tracks true physical attack CPS, physiological zero-delay clicks (≤ 10ms), and repetitive interval consistency across sliding windows (up to 500ms).
  - Tick-locked macro detection identifies hardware macros and cheat clients locked to client ticks (~50ms, ~100ms, ~200ms).
  - Rolling standard deviation (`sd`) analysis detects robotic regularity in combat clicks.
  - **Block Breaking Immunity**: Continuously tracks block digging phases (`START_DIGGING`, `FINISHED_DIGGING`, `CANCELLED_DIGGING`) and right-click actions, ensuring regular mining and block interaction will never false flag.

- **Strict Inventory & AutoTotem Protection (Full GrimAC Additions)**
  - `AutoTotem`: Real-time detection of instant totem transfers (0ms – 50ms upon totem pop), subtick multiswaps, and hotbar manipulation impossible for human reaction times.
  - `InventoryAutoClicker`: Flags robotic click consistency and excessive CPS inside GUI windows while providing native exemptions for vanilla `QUICK_CRAFT` (drag crafting) and `PICKUP_ALL` (double-click stacking).
  - `InventoryMove`: Detects illegal player movement, sprinting, or jumping while inventory containers are open.

- **Autonomous Phantom Combat Bots (`KillAuraBot`)**
  - Spawns dynamic, invisible phantom entity bots around suspicious players when combat suspicion accumulates.
  - Verifies whether player attacks target legitimate entities or lock onto server-side synthetic entities.

- **Statistical Ping Cohort Comparison (`ComparativeAnomaly`)**
  - Groups online players into latency cohorts (low, medium, high ping).
  - Dynamically compares click consistency, aim patterns, and transaction timing across players in similar network conditions to uncover subtle anomalies.

- **Adaptive Performance Protection**
  - Automatically monitors server TPS in real time.
  - When server TPS drops below a configurable threshold (default: `< 15.0`), the anticheat enters an emergency throttling mode:
    - Temporarily disables resource-intensive checks (inventory checks, phantom aura bots).
    - Reduces ray-tracing iterations on anti-xray modules.
    - Lowers check frequencies for secondary checks.
    - Broadcasts real-time warning notices to online operators with permission.
  - Automatically restores all checks and sends a recovery announcement once TPS climbs back above `17.0`.

---

## Compatibility

| Platform | Supported Versions | Implementation Details |
| :--- | :--- | :--- |
| **Folia** | 1.19.4 – 1.21.11+ | Native regional entity scheduling (`EntityScheduler`) & async task handlers |
| **Paper** | 1.8.8 – 1.21.11+ | Asynchronous Netty packet processing & asynchronous chunk tracking |
| **Purpur** | 1.8.8 – 1.21.11+ | Fully supported |
| **Spigot** | 1.8.8 – 1.21.11+ | Supported with standard Bukkit scheduler fallback |

---

## Commands and Permissions

### Commands

| Command | Description | Default Permission |
| :--- | :--- | :--- |
| `/grim help` | Displays the help menu and available commands | `grim.help` |
| `/grim alerts` | Toggles cheater violation alerts in chat | `grim.alerts` |
| `/grim verbose` | Toggles detailed real-time debug violation output | `grim.verbose` |
| `/grim perf` | Displays server TPS and performance protection status | `grim.perf` |
| `/grim spectate [player]` | Spectates a suspected player in first-person view | `grim.spectate` |
| `/grim profile <player>` | Displays transaction ping, latency history, and check statistics | `grim.profile` |
| `/grim history <player>` | Inspects past violation records and log data | `grim.history` |
| `/grim reload` | Asynchronously reloads all configuration and language files | `grim.reload` |
| `/grim version` | Displays current Full GrimAC build and version information | `grim.version` |

### Permissions

| Node | Description | Default Assignment |
| :--- | :--- | :--- |
| `grim.alerts` | Grants access to receive cheat violation notices | `op` |
| `grim.alerts.enable-on-join` | Automatically enables cheat violation alerts upon joining | `op` |
| `grim.perf` | Allows viewing performance protection status and notices | `op` |
| `grim.verbose` | Grants access to verbose check inspection | `op` |
| `grim.spectate` | Allows spectating suspected players | `op` |
| `grim.reload` | Allows reloading anticheat configuration files | `op` |
| `grim.exempt` | Fully exempts a player from all anticheat detections | `false` |
| `grim.nosetback` | Prevents movement rollbacks for authorized staff | `false` |

---

## Configuration

The default configuration file (`plugins/FullGrimAC/config.yml`):

```yaml
# =======================================================
#               Full GrimAC Configuration
# =======================================================

# Active language (en, ru, de, fr, es, it, ja, nl, pl, pt, ro, tr, zh)
language: "en"

# Performance Protection System
# Automatically throttles expensive checks during server lag
performance-protection:
  enabled: true
  min-tps: 15.0
  recovery-tps: 17.0
  disable-inventory-checks: true
  disable-killaura-bots: true
  reduce-raytrace-frequency: true
  notify-ops: true
```

---

## Localization

Full GrimAC includes built-in multi-language translations located in `plugins/FullGrimAC/messages/`:

| Code | Language | Native Name | File |
| :---: | :--- | :--- | :--- |
| `en` | English | English | `messages/en.yml` |
| `ru` | Russian | Русский | `messages/ru.yml` |
| `zh` | Chinese | 简体中文 | `messages/zh.yml` |
| `de` | German | Deutsch | `messages/de.yml` |
| `fr` | French | Français | `messages/fr.yml` |
| `es` | Spanish | Español | `messages/es.yml` |
| `it` | Italian | Italiano | `messages/it.yml` |
| `ja` | Japanese | 日本語 | `messages/ja.yml` |
| `nl` | Dutch | Nederlands | `messages/nl.yml` |
| `pl` | Polish | Polski | `messages/pl.yml` |
| `pt` | Portuguese | Português | `messages/pt.yml` |
| `ro` | Romanian | Română | `messages/ro.yml` |
| `tr` | Turkish | Türkçe | `messages/tr.yml` |

---

## Installation

1. Download `FullGrimAC-1.3.jar` from the release section.
2. Place the jar file into your server's `plugins/` directory.
3. Start or restart the server to generate default configurations.
4. Customize `plugins/FullGrimAC/config.yml` according to your server environment.
5. Execute `/grim reload` to apply configuration changes without server downtime.

---

## Compiling from Source

### Requirements
- Java Development Kit (JDK) 21 or higher
- Git

### Build Instructions

```bash
git clone https://github.com/NothingFlare/Full-GrimAC.git
cd Full-GrimAC
./gradlew :bukkit:shadowJar --no-daemon
```

The resulting compiled binary will be located in:
```text
bukkit/build/libs/FullGrimAC-1.3.jar
```

---

## License & Attribution

Full GrimAC is free and open-source software licensed under the **GNU General Public License v3.0 (GNU GPLv3)**.

- **Original GrimAC Project**:  
  Copyright (C) 2020–2024 GrimAC Team  
  Repository: [https://github.com/GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)

- **Full GrimAC Modifications & Additions**:  
  Copyright (C) 2026 NothingFlare  
  Part of Full GrimAC project.

For full license terms and conditions, refer to the [LICENSE](LICENSE) file.
