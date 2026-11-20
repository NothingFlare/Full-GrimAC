<div align="center">

# Full GrimAC

**An advanced, high-performance simulation anticheat fork for Paper, Purpur, Spigot, and Folia with enhanced combat, inventory, native Sonar AntiBot with map CAPTCHA, and dynamic performance protections.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.8--1.21.11+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-17+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-2.0-2b2b2b.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-GPL--3.0-2b2b2b.svg?style=flat-square)](LICENSE)

**Languages / Языки:** [English](#full-grimac) • [Русский](#full-grimac-русский)

[Overview](#overview) • [Fork Notice](#fork-notice) • [Key Features](#key-features) • [Compatibility](#compatibility) • [Commands and Permissions](#commands-and-permissions) • [Configuration](#configuration) • [Localization](#localization) • [Installation](#installation) • [Compiling from Source](#compiling-from-source) • [License & Attribution](#license--attribution)

</div>

---

## Overview

Full GrimAC is an open-source, modern simulation-based anticheat designed to provide rigorous cheat prevention across Minecraft versions 1.8 through 1.21.11+. By running asynchronous movement simulation on Netty network threads, Full GrimAC reconstructs player movement, world collisions, and player physics with mathematical precision, eliminating movement false positives while remaining impossible to bypass.

Building upon the robust foundation of the upstream GrimAC project, Full GrimAC introduces specialized packet-level detection modules, including advanced combat autoclicker analysis, instant totem and inventory swap exploit detection, autonomous phantom aura verification bots, an integrated native **Sonar** AntiBot system with map CAPTCHA, and an adaptive performance protection system designed for heavy server loads.

---

## Fork Notice

> [!IMPORTANT]
> **Full GrimAC is an independent open-source fork combining [GrimAC](https://github.com/GrimAnticheat/Grim) (originally developed by the GrimAC Team) and [Sonar](https://github.com/jonesdevelopment/sonar/) (originally developed by Sonar Contributors / Jones Development).**
> - This project is distributed under the terms of the **GNU General Public License v3.0 (GNU GPLv3)**.
> - Full GrimAC is **not** an official release of the upstream GrimAC project or Jones Development.
> - Modifications, enhancements, performance optimizers, and custom modules are developed and maintained by **NothingFlare**.
> - Inquiries, bug reports, and pull requests relating to this fork should be submitted directly to this repository.

---

## Key Features

- **1:1 Physics & Movement Simulation Engine (Inherited from GrimAC)**
  - Recreates the player's movement on the Netty thread, covering walking, swimming, knockback, cobwebs, bubble columns, and vehicle riding (boats, horses, striders).
  - Maintains a per-player asynchronous world replica using palette-compressed chunk caching.
  - Complete latency and inventory compensation prevents ghost blocks and desynchronization under high latency.
  - Full support for ViaVersion, ViaBackwards, and Geyser/Floodgate bedrock exemption.

- **Integrated Native Sonar AntiBot & Map CAPTCHA System**
  - Directly ports the complete, authentic Netty pipeline architecture of **[Sonar](https://github.com/jonesdevelopment/sonar/)** into Full GrimAC.
  - Zero server tick overhead: incoming connections are intercepted and verified at the Netty socket layer before ever touching the Minecraft server world or entities.
  - Multi-stage virtual limbo verification:
    - `GravityHandler`: Mathematical verification of fall motion ($\Delta Y$), terminal velocity, and block collision on an invisible platform.
    - `ProtocolHandler`: Vanilla protocol ping/transaction echo, client hotbar slot switching (`SetHeldItem`), and arm animation validation.
    - `VehicleHandler`: Vehicle interaction and packet sequence validation (boats/minecarts).
    - `CaptchaHandler`: In-game map CAPTCHA with procedural noise, ripple, and curve filters.
  - Verified player database caching (None, H2, MySQL, MariaDB, PostgreSQL) with automatic `TransferPacket` transfer on 1.20.5+.
  - All Sonar configuration and language files are stored in `plugins/FullGrimAC/sonar/`.

- **Entity Collision Speed Exploit Prevention (`EntitySpeed`)**
  - Detects players abusing entity pushing/collision physics to achieve impossible sprint and strafe acceleration.
  - Predicts maximum physical push forces and clamps uncertainty bounds to prevent entity-speed bypasses while maintaining full setback stability.

- **Advanced Combat & Click Consistency Detection (Full GrimAC Additions)**
  - `CombatAutoClicker`: Enhanced algorithm with instant detection for blatant CPS (>= 22), high CPS, zero-delay clicks, and strict standard deviation tracking (window: 10 clicks, threshold: 3.5ms).
  - Packet deduplication resilience: Standardizes on animation ticks as the authoritative clock, preventing attack packet bursts from disrupting interval tracking.
  - Mining & block break tap immunity: Sustained mining protection ensures legit mining and block interactions will never cause false flags.
  - Rolling standard deviation (`sd`) analysis detects robotic regularity in combat clicks.

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

- **Interactive In-Game Settings GUI (`/grim gui`)**
  - 3-page interactive chest inventory interface:
    - **Page 1 (1/3)**: Primary combat, movement, and block checks with individual toggles.
    - **Page 2 (2/3)**: Additional packet, vehicle, exploit, and chat checks.
    - **Page 3 (3/3)**: Server optimization toggles (TPS Protection, Redstone, TNT, Disk).
  - Master switch in slot 44 (`ALL_CHECKS`) to toggle all checks simultaneously.
  - Instant synchronization for all connected players with asynchronous persistence to `gui-settings.yml`.

- **Server Optimizations & Performance Protection**
  - **Adaptive TPS Protection**: Automatically monitors server TPS. When TPS drops below `< 15.0`, it temporarily throttles heavy checks and phantom bots, restoring full checks when TPS recovers above `17.0`.
  - **Smart Disk & World Save Optimizer (`DiskOptimizer`)**: Eliminates disk save lag by temporarily suspending auto-save on the exact autosave tick only for worlds untouched by players, while keeping chunk unloads running smoothly every tick to prevent memory leaks.
  - **Redstone Clock & Feedback Limiter (`RedstoneOptimizer`)**: Throttles runaway redstone clocks and deduplicates identical signal updates in the same tick.
  - **TNT Physics & Blast Optimizer (`TNTOptimizer`)**: Caches dense multi-TNT blast craters and skips redundant block damage calculations in water chambers.

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
| `/sonar [stats\|blacklist\|verified\|verbose\|dump\|reload\|notify]` | Full Sonar AntiBot management, statistics, and configuration | `sonar.command` |
| `/grim gui` | Opens the graphical settings menu for checks and server optimizations | `grim.gui` |
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
| `sonar.command` | Grants access to manage Sonar AntiBot system and view statistics | `op` |
| `grim.gui` | Allows opening the in-game anticheat configuration GUI | `op` |
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

# Active language (en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko)
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

| Code | Language | Native Name | Configuration | Check Descriptions |
| :---: | :--- | :--- | :---: | :---: |
| `en` | English | English | Supported | Supported |
| `ru` | Russian | Русский | Supported | Supported |
| `zh` | Chinese | 简体中文 | Supported | Supported |
| `de` | German | Deutsch | Supported | Supported |
| `fr` | French | Français | Supported | Supported |
| `es` | Spanish | Español | Supported | Supported |
| `pt` | Portuguese | Português | Supported | Supported |
| `ja` | Japanese | 日本語 | Supported | Supported |
| `uk` | Ukrainian | Українська | Supported | Supported |
| `it` | Italian | Italiano | Supported | Supported |
| `pl` | Polish | Polski | Supported | Supported |
| `tr` | Turkish | Türkçe | Supported | Supported |
| `ko` | Korean | 한국어 | Supported | Supported |

> [!NOTE]
> All technical check identifiers (e.g. `CombatAutoClicker`, `Simulation`, `EntitySpeed`) remain strictly in English across all alerts and commands, while check descriptions displayed on hover (`%description%`) are fully localized into all 13 supported languages.

---

## Installation

1. Download `FullGrimAC-2.0.jar` from the release section.
2. Place the jar file into your server's `plugins/` directory.
3. Start or restart the server to generate default configurations.
4. Customize `plugins/FullGrimAC/config.yml` and `plugins/FullGrimAC/sonar/config.yml` according to your server environment.
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
./gradlew assemble
```

The resulting compiled binary will be located in:
```text
bukkit/build/libs/FullGrimAC-2.0.jar
```

---

## License & Attribution

Full GrimAC is free and open-source software licensed under the **GNU General Public License v3.0 (GNU GPLv3)**.

- **Original GrimAC Project**:  
  Copyright (C) 2020–2024 GrimAC Team  
  Repository: [https://github.com/GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)

- **Sonar AntiBot & Captcha System**:  
  Copyright (C) 2025 Sonar Contributors  
  Repository: [https://github.com/jonesdevelopment/sonar/](https://github.com/jonesdevelopment/sonar/)

- **Full GrimAC Modifications & Additions**:  
  Copyright (C) 2026 NothingFlare  
  Part of Full GrimAC project.

For full license terms and conditions, refer to the [LICENSE](LICENSE) file.

---
---

<div align="center">

# Full GrimAC (Русский)

**Продвинутый, высокопроизводительный симуляционный античит (форк GrimAC и Sonar) для Paper, Purpur, Spigot и Folia с улучшенным боевым анализом, защитой инвентаря, встроенным антиботом с капчей на картах и динамической защитой производительности.**

[![Версия Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21.11+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Версия Java](https://img.shields.io/badge/Java-17+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Поддерживается-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Релиз](https://img.shields.io/badge/Релиз-2.0-2b2b2b.svg?style=flat-square)]()
[![Лицензия](https://img.shields.io/badge/Лицензия-GPL--3.0-2b2b2b.svg?style=flat-square)](LICENSE)

**Языки / Languages:** [English](#full-grimac) • [Русский](#full-grimac-русский)

[Обзор](#обзор) • [Уведомление о форках](#уведомление-о-форках) • [Ключевые возможности](#ключевые-возможности) • [Совместимость](#совместимость) • [Команды и права](#команды-и-права) • [Конфигурация](#конфигурация) • [Локализация](#локализация) • [Установка](#установка) • [Сборка из исходников](#сборка-из-исходников) • [Лицензия и авторские права](#лицензия-и-авторские-права)

</div>

---

## Обзор

Full GrimAC — это современный симуляционный античит с открытым исходным кодом, созданный для бескомпромиссного предотвращения использования читов на версиях Minecraft от 1.8 до 1.21.11+. Работая в асинхронных потоках Netty, Full GrimAC с математической точностью воссоздает движение игрока, столкновения с миром и физику блоков, исключая ложные срабатывания на движение и делая обход проверок невозможным.

Опираясь на фундамент проекта GrimAC, Full GrimAC включает специализированные пакетные модули обнаружения: продвинутый анализ боевых автокликеров, мгновенную детекцию свапов тотемов и манипуляций инвентарем, автономных фантомных ботов проверки киллауры, встроенную систему антибота **Sonar** с генерацией капчи на картах и адаптивную систему защиты от просадок TPS при высоких нагрузках.

---

## Уведомление о форках

> [!IMPORTANT]
> **Full GrimAC является независимым форком с открытым исходным кодом, объединяющим наработки проектов [GrimAC](https://github.com/GrimAnticheat/Grim) (команда разработчиков GrimAC) и [Sonar](https://github.com/jonesdevelopment/sonar/) (разработчики Sonar Contributors / Jones Development).**
> - Проект распространяется на условиях лицензии **GNU General Public License v3.0 (GNU GPLv3)**.
> - Full GrimAC **не** является официальным релизом команд разработчиков оригинального GrimAC или Jones Development.
> - Модификации, улучшения, оптимизаторы производительности и кастомные модули разрабатываются и поддерживаются автором **NothingFlare**.
> - Все вопросы, сообщения об ошибках и пулл-реквесты направляются непосредственно в данный репозиторий.

---

## Ключевые возможности

- **Движок симуляции физики и движения 1:1 (унаследован от GrimAC)**
  - Точное воспроизведение движений игрока в потоке Netty: бег, плавание, откидывание (velocity), паутина, пузырьковые колонны, езда на транспорте (лодки, лошади, страйдеры).
  - Асинхронная копия мира для каждого игрока с палитровым сжатием чанков.
  - Компенсация задержки (ping/latency) и состояния инвентаря исключает появление фантомных блоков и рассинхронизацию.
  - Полная поддержка ViaVersion, ViaBackwards и исключений для игроков Bedrock (Geyser/Floodgate).

- **Встроенная нативная система антибота Sonar с капчей на картах**
  - Полный перенос архитектуры конвейера Netty из проекта **[Sonar](https://github.com/jonesdevelopment/sonar/)** прямо в Full GrimAC.
  - Нулевая нагрузка на серверные тики: входящие подключения перехватываются и проверяются на уровне сокетов Netty до создания игрока в мире сервера.
  - Многоступенчатая верификация в виртуальном лимбо:
    - `GravityHandler`: Математический анализ гравитации ($\Delta Y$), терминальной скорости и коллизий с невидимой платформой.
    - `ProtocolHandler`: Эхо-ответы на транзакции и пинг ванильного протокола, переключение слотов хотбара (`SetHeldItem`) и взмахи рукой.
    - `VehicleHandler`: Проверка взаимодействия с транспортом (лодки/вагонетки) и валидация пакетов.
    - `CaptchaHandler`: Внутриигровая капча на карте с процедурным шумом, волнами и кривыми искажения.
  - База данных верифицированных игроков (None, H2, MySQL, MariaDB, PostgreSQL) с автоматической передачей пакетом `TransferPacket` на 1.20.5+.
  - Все конфигурации и языковые файлы Sonar хранятся в `plugins/FullGrimAC/sonar/`.

- **Детекция ускорения от столкновения с сущностями (`EntitySpeed`)**
  - Обнаружение игроков, использующих физику расталкивания сущностей/мобов для получения невозможного ускорения при беге и стрейфах.
  - Расчет предельных физических сил отталкивания с ограничением погрешностей для стабильного возврата (setback).

- **Продвинутый боевой анализ и детекция автокликеров (модули Full GrimAC)**
  - `CombatAutoClicker`: Мгновенное срабатывание при запредельном CPS (>= 22), высоком среднем CPS, физиологически невозможных нулевых задержках (0–10 мс) и анализе стандартного отклонения (окно: 10 кликов, порог: 3.5 мс).
  - Устойчивость к склейке пакетов: Синхронизация по тикам анимации взмаха предотвращает ложные срабатывания при скачках сети.
  - Иммунитет при копании: Отслеживание фаз разрушения блоков (`START_DIGGING`, `FINISHED_DIGGING`, `CANCELLED_DIGGING`) исключает ложные флаги во время майнинга.
  - Скользящее стандартное отклонение (`sd`) выявляет роботизированную стабильность кликов.

- **Строгая защита инвентаря и авто-тотема (модули Full GrimAC)**
  - `AutoTotem`: Анализ перемещения тотема в реальном времени (0–50 мс после срабатывания), субтиковые мультисвапы и неестественные манипуляции хотбаром.
  - `InventoryAutoClicker`: Детекция неестественной скорости и периодичности кликов в окнах GUI со встроенным исключением для ванильного быстрого крафта (`QUICK_CRAFT`) и сбора стака (`PICKUP_ALL`).
  - `InventoryMove`: Запрет на нелегитимное передвижение, бег или прыжки с открытыми контейнерами.

- **Автономные фантомные боты (`KillAuraBot`)**
  - При накоплении боевых подозрений вокруг игрока спавнятся невидимые фантомные сущности.
  - Проверка, атакует ли игрок реальные цели или его прицел «прилипает» к синтетическим сущностям античита.

- **Когортный статистический анализ пинга (`ComparativeAnomaly`)**
  - Разделение игроков на группы по величине задержки (низкий, средний, высокий пинг).
  - Сравнение стабильности кликов, динамики наводки и откликов на транзакции между игроками со схожими сетевыми условиями.

- **Интерактивное внутриигровое GUI-меню настроек (`/grim gui`)**
  - 3-страничное графическое меню настроек в инвентаре:
    - **Страница 1 (1/3)**: Основные боевые, двигательные и блочные проверки с раздельными переключателями.
    - **Страница 2 (2/3)**: Дополнительные пакетные проверки, транспорт, эксплойты и спам-фильтры чата.
    - **Страница 3 (3/3)**: Управление оптимизациями сервера (Защита TPS, Редстоун, ТНТ, Диск).
  - Мастер-переключатель в слоте 44 (`ALL_CHECKS`) для одновременного включения/выключения всех проверок.
  - Моментальное применение изменений для всех игроков онлайн с сохранением в `gui-settings.yml`.

- **Оптимизации сервера и защита производительности**
  - **Адаптивная защита TPS**: Постоянный мониторинг тиков. При просадке TPS ниже `< 15.0` временно снижается нагрузка ресурсоемких проверок и ботов; при возврате выше `17.0` проверки автоматически возвращаются в полный режим.
  - **Умное сохранение миров (`DiskOptimizer`)**: Устраняет дисковые фризы сервера, временно отключая автосейв на тике сохранения только для тех миров, в которых игроки не совершали действий, при этом сохраняя постоянную выгрузку неиспользуемых чанков каждый тик для защиты от утечек оперативной памяти (OOM).
  - **Ограничитель редстоун-машин (`RedstoneOptimizer`)**: Дросселирует цикличные генераторы импульсов и дедуплицирует повторяющиеся сигналы в пределах одного тика.
  - **Оптимизатор динамита (`TNTOptimizer`)**: Кеширует зоны одновременных взрывов ТНТ и отсекает холостые расчеты урона блокам в воде.

---

## Совместимость

| Платформа | Поддерживаемые версии | Особенности интеграции |
| :--- | :--- | :--- |
| **Folia** | 1.19.4 – 1.21.11+ | Нативный региональный шедулер сущностей (`EntityScheduler`) и асинхронные задачи |
| **Paper** | 1.8.8 – 1.21.11+ | Асинхронная обработка пакетов Netty и асинхронный трекинг чанков |
| **Purpur** | 1.8.8 – 1.21.11+ | Полная поддержка |
| **Spigot** | 1.8.8 – 1.21.11+ | Поддержка со стандартным шедулером Bukkit |

---

## Команды и права

### Команды

| Команда | Описание | Право по умолчанию |
| :--- | :--- | :--- |
| `/sonar [stats\|blacklist\|verified\|verbose\|dump\|reload\|notify]` | Управление антиботом Sonar, статистика и настройки | `sonar.command` |
| `/grim gui` | Открывает графическое меню настроек проверок и оптимизаций | `grim.gui` |
| `/grim help` | Показывает справочное меню и доступные команды | `grim.help` |
| `/grim alerts` | Включает/выключает оповещения о нарушениях читеров в чате | `grim.alerts` |
| `/grim verbose` | Включает детальный отладочный вывод проверок в реальном времени | `grim.verbose` |
| `/grim perf` | Показывает текущий TPS сервера и статус защиты производительности | `grim.perf` |
| `/grim spectate [игрок]` | Наблюдение за подозреваемым от первого лица | `grim.spectate` |
| `/grim profile <игрок>` | Показывает пинг по транзакциям, историю пинга и статистику проверок | `grim.profile` |
| `/grim history <игрок>` | Просмотр истории прошлых нарушений и логов игрока | `grim.history` |
| `/grim reload` | Асинхронно перезагружает файлы конфигурации и языковые пакеты | `grim.reload` |
| `/grim version` | Отображает версию сборки Full GrimAC | `grim.version` |

### Права (Permissions)

| Право | Описание | Назначение по умолчанию |
| :--- | :--- | :--- |
| `sonar.command` | Доступ к управлению антиботом Sonar и просмотру статистики | `op` |
| `grim.gui` | Разрешает открытие внутриигрового меню GUI | `op` |
| `grim.alerts` | Получение уведомлений о нарушениях читеров | `op` |
| `grim.alerts.enable-on-join` | Автоматическое включение оповещений при входе на сервер | `op` |
| `grim.perf` | Просмотр статуса системы защиты TPS и уведомлений | `op` |
| `grim.verbose` | Доступ к подробному вербоз-режиму проверок | `op` |
| `grim.spectate` | Разрешает наблюдение за подозреваемыми игроками | `op` |
| `grim.reload` | Разрешает перезагрузку конфигурации плагина | `op` |
| `grim.exempt` | Полный иммунитет игрока ко всем проверкам античита | `false` |
| `grim.nosetback` | Отключает возврат (setback) при детекциях для персонала | `false` |

---

## Конфигурация

Основной конфигурационный файл (`plugins/FullGrimAC/config.yml`):

```yaml
# =======================================================
#               Конфигурация Full GrimAC
# =======================================================

# Язык интерфейса (en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko)
language: "ru"

# Система защиты производительности
# Автоматически снижает нагрузку тяжелых проверок при просадках TPS
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

## Локализация

Full GrimAC поддерживает встроенные многоязычные переводы, расположенные в `plugins/FullGrimAC/messages/`:

| Код | Язык | Оригинальное название | Конфигурация | Описания проверок |
| :---: | :--- | :--- | :---: | :---: |
| `en` | Английский | English | Поддерживается | Поддерживается |
| `ru` | Русский | Русский | Поддерживается | Поддерживается |
| `zh` | Китайский | 简体中文 | Поддерживается | Поддерживается |
| `de` | Немецкий | Deutsch | Поддерживается | Поддерживается |
| `fr` | Французский | Français | Поддерживается | Поддерживается |
| `es` | Испанский | Español | Поддерживается | Поддерживается |
| `pt` | Португальский | Português | Поддерживается | Поддерживается |
| `ja` | Японский | 日本語 | Поддерживается | Поддерживается |
| `uk` | Украинский | Українська | Поддерживается | Поддерживается |
| `it` | Итальянский | Italiano | Поддерживается | Поддерживается |
| `pl` | Польский | Polski | Поддерживается | Поддерживается |
| `tr` | Турецкий | Türkçe | Поддерживается | Поддерживается |
| `ko` | Корейский | 한국어 | Поддерживается | Поддерживается |

> [!NOTE]
> Все технические идентификаторы проверок (например, `CombatAutoClicker`, `Simulation`, `EntitySpeed`) остаются на английском языке во всех алертах и командах для однозначности, в то время как подробные описания проверок при наведении курсора (`%description%`) полностью локализованы на все 13 поддерживаемых языков.

---

## Установка

1. Скачайте `FullGrimAC-2.0.jar` из раздела релизов.
2. Поместите jar-файл в папку `plugins/` вашего сервера.
3. Запустите или перезапустите сервер для генерации стандартных конфигураций.
4. Настройте файлы `plugins/FullGrimAC/config.yml` и `plugins/FullGrimAC/sonar/config.yml` под требования вашего проекта.
5. Выполните команду `/grim reload` для применения изменений без перезагрузки сервера.

---

## Сборка из исходников

### Требования
- Java Development Kit (JDK) 21 или выше
- Git

### Инструкция по сборке

```bash
git clone https://github.com/NothingFlare/Full-GrimAC.git
cd Full-GrimAC
./gradlew assemble
```

Скомпилированный файл плагина будет находиться по пути:
```text
bukkit/build/libs/FullGrimAC-2.0.jar
```

---

## Лицензия и авторские права

Full GrimAC является свободным программным обеспечением с открытым исходным кодом под лицензией **GNU General Public License v3.0 (GNU GPLv3)**.

- **Оригинальный проект GrimAC**:  
  Copyright (C) 2020–2024 GrimAC Team  
  Репозиторий: [https://github.com/GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)

- **Система антибота и капчи Sonar**:  
  Copyright (C) 2025 Sonar Contributors  
  Репозиторий: [https://github.com/jonesdevelopment/sonar/](https://github.com/jonesdevelopment/sonar/)

- **Модификации и дополнения Full GrimAC**:  
  Copyright (C) 2026 NothingFlare  
  Часть проекта Full GrimAC.

Полный текст лицензии доступен в файле [LICENSE](LICENSE).
