/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.gui;

import ac.grim.grimac.checks.CheckToggleRegistry;
import ac.grim.grimac.manager.performance.PerformanceProtectionManager;
import ac.grim.grimac.platform.api.gui.PlatformGuiHandler;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.gui.optimization.DiskOptimizer;
import ac.grim.grimac.platform.bukkit.gui.optimization.RedstoneOptimizer;
import ac.grim.grimac.platform.bukkit.gui.optimization.TNTOptimizer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GrimGuiManager implements PlatformGuiHandler, Listener {

    private static GrimGuiManager instance;
    private final Object saveLock = new Object();

    public record CheckEntry(int slot, String checkName, String displayName, Material material, String description) {}

    private final List<CheckEntry> page1Checks = new ArrayList<>();
    private final List<CheckEntry> page2Checks = new ArrayList<>();

    public GrimGuiManager() {
        instance = this;
        registerCheckEntries();
        loadSettings();
    }

    public static GrimGuiManager getInstance() {
        return instance;
    }

    private void registerCheckEntries() {
        // ==========================================
        // PAGE 1: Основные боевые, двигательные и мировые проверки
        // ==========================================
        page1Checks.add(new CheckEntry(0, "Reach", "Дальность атаки (Reach)", Material.DIAMOND_SWORD, "Проверка на увеличенную дистанцию удара (> 3.0 блоков)"));
        page1Checks.add(new CheckEntry(1, "Hitboxes", "Хитбоксы (Hitboxes)", Material.IRON_SWORD, "Проверка на удары вне физического хитбокса сущности"));
        page1Checks.add(new CheckEntry(2, "KillAuraBot", "Киллаура (Боты)", Material.NETHERITE_SWORD, "Детекция авто-наводки и комбат-ботов"));
        page1Checks.add(new CheckEntry(3, "CombatAutoClicker", "Боевой автокликер", Material.WOODEN_SWORD, "Детекция макросов, высокого CPS и неестественных интервалов"));
        page1Checks.add(new CheckEntry(4, "AimModulo360", "Анализ наводки (Aim)", Material.BOW, "Проверка на неестественные довороты головы и аимбот"));
        page1Checks.add(new CheckEntry(5, "AutoTotem", "Авто-тотем", Material.TOTEM_OF_UNDYING, "Детекция мгновенной смены тотема бессмертия при срабатывании"));
        page1Checks.add(new CheckEntry(6, "SelfInteract", "Атака себя", Material.SHIELD, "Проверка на пакеты взаимодействия с самим собой"));
        page1Checks.add(new CheckEntry(7, "AntiKB", "Анти-откидывание (Velocity)", Material.ANVIL, "Проверка на получение ожидаемого импульса откидывания"));
        page1Checks.add(new CheckEntry(8, "NoFall", "Анти-урон от падения (NoFall)", Material.FEATHER, "Проверка на сброс дистанции падения и спуфинг земли"));
        page1Checks.add(new CheckEntry(9, "Phase", "Проход сквозь блоки (Phase)", Material.ENDER_PEARL, "Проверка на перемещение сквозь сплошные блоки и коллизии"));
        page1Checks.add(new CheckEntry(10, "EntitySpeed", "Спидхак на сущностях", Material.SUGAR, "Детекция ускорения от коллизий с мобами и игроками"));
        page1Checks.add(new CheckEntry(11, "GroundSpoof", "Подделка земли (GroundSpoof)", Material.LEATHER_BOOTS, "Проверка флага onGround при нахождении в воздухе"));
        page1Checks.add(new CheckEntry(12, "NoSlow", "Замедление (NoSlow)", Material.SOUL_SAND, "Проверка снижения скорости при еде, стрельбе и паутине"));
        page1Checks.add(new CheckEntry(13, "SprintA", "Спринт (Sprint)", Material.GOLDEN_BOOTS, "Проверка на бег с низким голодом, в блоках или приседе"));
        page1Checks.add(new CheckEntry(14, "ElytraA", "Полет на элитрах (Elytra)", Material.FIREWORK_ROCKET, "Проверка физики и скорости полета на элитрах"));
        page1Checks.add(new CheckEntry(15, "VehicleA", "Читы на транспорте (Vehicle)", Material.OAK_BOAT, "Проверка перемещения на лодках, лошадях и вагонетках"));
        page1Checks.add(new CheckEntry(16, "Timer", "Таймер (Ускорение времени)", Material.CLOCK, "Проверка на манипуляцию скоростью игрового времени"));
        page1Checks.add(new CheckEntry(17, "TickTimer", "Тик-таймер (TickTimer)", Material.COMPASS, "Проверка соответствия тиков клиента серверному времени"));
        page1Checks.add(new CheckEntry(18, "FastBreak", "Быстрое ломание (FastBreak)", Material.DIAMOND_PICKAXE, "Проверка скорости разрушения блоков инструментом"));
        page1Checks.add(new CheckEntry(19, "FarBreak", "Дальнее ломание (FarBreak)", Material.GOLDEN_PICKAXE, "Проверка на разрушение блоков за пределами досягаемости"));
        page1Checks.add(new CheckEntry(20, "WrongBreak", "Неверное ломание (WrongBreak)", Material.IRON_PICKAXE, "Проверка порядка пакетов начала и завершения копания"));
        page1Checks.add(new CheckEntry(21, "AirLiquidBreak", "Ломание воздуха (AirBreak)", Material.SHEARS, "Проверка на попытку разрушить воздух или жидкости"));
        page1Checks.add(new CheckEntry(22, "NoSwingBreak", "Ломание без анимации (NoSwing)", Material.STICK, "Проверка анимации взмаха руки при разрушении блоков"));
        page1Checks.add(new CheckEntry(23, "MultiPlace", "Быстрая установка (FastPlace)", Material.BRICKS, "Проверка на установку нескольких блоков за один тик"));
        page1Checks.add(new CheckEntry(24, "FarPlace", "Дальняя установка (FarPlace)", Material.COBBLESTONE, "Проверка на установку блоков на недопустимой дистанции"));
        page1Checks.add(new CheckEntry(25, "AirLiquidPlace", "Установка в воздухе (AirPlace)", getSafeMaterial("SCAFFOLDING", Material.DIRT), "Проверка на установку блоков без опоры на твердые грани"));
        page1Checks.add(new CheckEntry(26, "PositionPlace", "Установка за стеной", getSafeMaterial("TARGET", Material.REDSTONE_BLOCK), "Проверка видимости грани блока при установке"));
        page1Checks.add(new CheckEntry(27, "RotationPlace", "Установка без взгляда", getSafeMaterial("CROSSBOW", Material.BOW), "Проверка направления взгляда игрока на грань блока"));
        page1Checks.add(new CheckEntry(28, "InventoryAutoClicker", "Кликер в сундуках (InvClick)", Material.CHEST, "Детекция авто-кликов и перемещения предметов в инвентаре"));
        page1Checks.add(new CheckEntry(29, "InventoryAction", "Движение в инвентаре", Material.HOPPER, "Проверка перемещения и спринта при открытом контейнере"));
        page1Checks.add(new CheckEntry(30, "BadPacketsA", "Неверные пакеты (BadPackets)", Material.REPEATER, "Проверка валидности и допустимости клиентских пакетов"));
        page1Checks.add(new CheckEntry(31, "PacketOrderA", "Порядок пакетов (PacketOrder)", Material.COMPARATOR, "Проверка правильной последовательности транзакций и пакетов"));
        page1Checks.add(new CheckEntry(32, "CrashA", "Защита от крашеров (Crash)", Material.BARRIER, "Блокировка пакетов, вызывающих перегрузку или краш сервера"));
        page1Checks.add(new CheckEntry(33, "ExploitA", "Пакетные эксплойты (Exploit)", Material.BEDROCK, "Защита от известных уязвимостей сетевого протокола"));
        page1Checks.add(new CheckEntry(34, "AntiXray", "Защита от X-Ray", Material.DIAMOND_ORE, "Анализ подозрительного нахождения скрытых руд"));
        page1Checks.add(new CheckEntry(35, "MultiActionsA", "Мульти-действия (MultiActions)", Material.GOLDEN_AXE, "Проверка выполнения несовместимых действий одновременно"));
        page1Checks.add(new CheckEntry(36, "SneakingEstimator", "Скрытность (Sneak)", Material.LEATHER_LEGGINGS, "Проверка состояния приседания и коллизий высоты"));
        page1Checks.add(new CheckEntry(37, "GhostBlockMitigation", "Фантомные блоки (GhostBlock)", Material.PRISMARINE_SHARD, "Автоматическая синхронизация рассинхронизированных блоков"));
        page1Checks.add(new CheckEntry(38, "ComparativeAnomaly", "Аномалии движения", getSafeMaterial("AMETHYST_SHARD", Material.EMERALD), "Статистический анализ отклонений в поведении и пинге"));
        page1Checks.add(new CheckEntry(39, "AntiExplosion", "Взрывной импульс (Explosion)", Material.FIRE_CHARGE, "Проверка корректности отталкивания при взрывах"));
        page1Checks.add(new CheckEntry(40, "VehicleTimer", "Таймер в транспорте", Material.MINECART, "Проверка частоты пакетов при езде на сущностях"));
        page1Checks.add(new CheckEntry(41, "PositionBreakA", "Копание сквозь стены", getSafeMaterial("WARPED_FUNGUS_ON_A_STICK", Material.CARROT_ON_A_STICK), "Проверка линии взгляда до разрушаемого блока"));
        page1Checks.add(new CheckEntry(42, "FabricatedPlace", "Поддельная установка", Material.DISPENSER, "Проверка подлинности грани установки блока"));
        page1Checks.add(new CheckEntry(43, "BadPacketsF", "Спуфинг слотов хотбара", Material.ITEM_FRAME, "Проверка смены активного слота хотбара"));
        page1Checks.add(new CheckEntry(44, "ALL_CHECKS", "Все проверки", Material.EMERALD_BLOCK, "Мастер-переключатель: включить или выключить все проверки одновременно"));

        // ==========================================
        // PAGE 2: Дополнительные пакетные, транспортные, инвентарные проверки и чат
        // ==========================================
        page2Checks.add(new CheckEntry(0, "GhostItemCheck", "Фантомные предметы (GhostItem)", Material.CHEST_MINECART, "Синхронизация инвентаря при установке блоков в хитбоксы сущностей"));
        page2Checks.add(new CheckEntry(1, "BadPacketsC", "Недопустимый статус (BadPacketsC)", Material.PAPER, "Проверка правильности смены статусов сущности игрока"));
        page2Checks.add(new CheckEntry(2, "BadPacketsG", "Пакеты сущностей (BadPacketsG)", getSafeMaterial("GLOW_ITEM_FRAME", Material.ITEM_FRAME), "Проверка валидности пакетов взаимодействия с сущностями"));
        page2Checks.add(new CheckEntry(3, "BadPacketsI", "Спуфинг инвентаря (BadPacketsI)", Material.ENDER_CHEST, "Проверка некорректных взаимодействий со слотами инвентаря"));
        page2Checks.add(new CheckEntry(4, "BadPacketsK", "Позиция транспорта (BadPacketsK)", Material.SADDLE, "Проверка координат при посадке и спешивании с транспорта"));
        page2Checks.add(new CheckEntry(5, "BadPacketsM", "Действия игрока (BadPacketsM)", Material.BLAZE_POWDER, "Проверка последовательности начала и отмены действий"));
        page2Checks.add(new CheckEntry(6, "BadPacketsY", "Неверный слот (BadPacketsY)", getSafeMaterial("RECOVERY_COMPASS", Material.COMPASS), "Проверка недопустимой смены активных предметов"));
        page2Checks.add(new CheckEntry(7, "BadPacketsZ", "Спуфинг снарядов (BadPacketsZ)", Material.SNOWBALL, "Проверка валидности пакетов броска снарядов и жемчугов"));
        page2Checks.add(new CheckEntry(8, "PacketOrderB", "Транзакции инвентаря (PacketOrderB)", Material.REDSTONE_TORCH, "Проверка последовательности транзакций в контейнерах"));
        page2Checks.add(new CheckEntry(9, "PacketOrderC", "Синхронизация слотов (PacketOrderC)", Material.REDSTONE, "Проверка правильного порядка ответа на синхронизацию"));
        page2Checks.add(new CheckEntry(10, "PacketOrderD", "Порядок способностей (PacketOrderD)", Material.NETHER_STAR, "Проверка согласованности пакетов способностей и полета"));
        page2Checks.add(new CheckEntry(11, "PacketOrderO", "Транзакции мира (PacketOrderO)", Material.DAYLIGHT_DETECTOR, "Проверка последовательности пакетов мира и чанков"));
        page2Checks.add(new CheckEntry(12, "MultiActionsC", "Мульти-действия C", Material.IRON_AXE, "Проверка одновременной атаки и использования предметов"));
        page2Checks.add(new CheckEntry(13, "MultiActionsD", "Мульти-действия D", Material.DIAMOND_AXE, "Проверка одновременного разрушения и копания блоков"));
        page2Checks.add(new CheckEntry(14, "MultiActionsE", "Мульти-действия E", Material.NETHERITE_AXE, "Проверка недопустимых комбинаций действий в воздухе"));
        page2Checks.add(new CheckEntry(15, "VehicleB", "Физика транспорта B", Material.MINECART, "Проверка движения транспорта и скорости лодок/вагонеток"));
        page2Checks.add(new CheckEntry(16, "VehicleD", "Позиция транспорта D", Material.FURNACE_MINECART, "Проверка рассинхронизации позиции при движении на сущности"));
        page2Checks.add(new CheckEntry(17, "ExploitB", "Пакетные эксплойты B", getSafeMaterial("COMMAND_BLOCK", Material.IRON_BLOCK), "Защита от эксплойтов длинных пакетов и NBT-тегов"));
        page2Checks.add(new CheckEntry(18, "BadPacketsD", "Спуфинг поворота головы", Material.ENDER_EYE, "Проверка неестественных углов поворота pitch и yaw"));
        page2Checks.add(new CheckEntry(19, "BadPacketsE", "Смена предметов (BadPacketsE)", Material.SHEARS, "Проверка мгновенного свапа предметов в хотбаре"));
        page2Checks.add(new CheckEntry(20, "BadPacketsJ", "Угол атаки (BadPacketsJ)", Material.SPECTRAL_ARROW, "Проверка соответствия вектора взгляда направлению удара"));
        page2Checks.add(new CheckEntry(21, "BadPacketsL", "Анимация взмаха (BadPacketsL)", Material.BLAZE_ROD, "Проверка порядка пакетов анимации руки перед атакой"));
        page2Checks.add(new CheckEntry(22, "BadPacketsO", "Пакеты движения (BadPacketsO)", Material.REDSTONE_BLOCK, "Проверка корректности пакетов телепортации и подтверждения"));
        page2Checks.add(new CheckEntry(23, "BadPacketsP", "Коллизии блоков (BadPacketsP)", Material.SLIME_BLOCK, "Проверка спуфинга коллизий и застревания в блоках"));
        page2Checks.add(new CheckEntry(24, "BadPacketsQ", "Использование предметов", Material.GOLDEN_APPLE, "Проверка таймингов еды, питья и стрельбы из лука"));
        page2Checks.add(new CheckEntry(25, "BadPacketsR", "Взаимодействие с блоками", Material.DROPPER, "Проверка пакетов взаимодействия на грани блоков"));
        page2Checks.add(new CheckEntry(26, "BadPacketsS", "Установка блоков (BadPacketsS)", Material.HOPPER, "Проверка допустимости порядка пакетов установки"));
        page2Checks.add(new CheckEntry(27, "BadPacketsU", "Валидация пакетов (BadPacketsU)", Material.BOOK, "Проверка структуры пакетов полезной нагрузки клиента"));
        page2Checks.add(new CheckEntry(28, "BadPacketsV", "Транспорт (BadPacketsV)", Material.TNT_MINECART, "Проверка неестественного импульса транспортных средств"));
        page2Checks.add(new CheckEntry(29, "InvalidInteractCursor", "Курсор клика (Cursor)", Material.CROSSBOW, "Проверка позиции курсора на грани блока при установке"));
        page2Checks.add(new CheckEntry(30, "ChatA", "Флуд чатом (ChatA)", Material.WRITABLE_BOOK, "Защита от массового спама сообщениями в чат"));
        page2Checks.add(new CheckEntry(31, "ChatB", "Быстрый чат (ChatB)", Material.FEATHER, "Проверка неестественно быстрого ввода текстовых команд"));
        page2Checks.add(new CheckEntry(32, "ChatC", "Спецсимволы (ChatC)", Material.NAME_TAG, "Блокировка невидимых и краш-символов в чате"));
        page2Checks.add(new CheckEntry(33, "ChatD", "Повторы чата (ChatD)", getSafeMaterial("KNOWLEDGE_BOOK", Material.BOOK), "Фильтрация дублирующихся сообщений от ботов"));
        page2Checks.add(new CheckEntry(44, "ALL_CHECKS", "Все проверки", Material.EMERALD_BLOCK, "Мастер-переключатель: включить или выключить все проверки одновременно"));
    }

    private static Material getSafeMaterial(String name, Material fallback) {
        try {
            Material mat = Material.matchMaterial(name);
            if (mat != null) return mat;
        } catch (Throwable ignored) {}
        return fallback;
    }

    @Override
    public void openGui(Sender sender) {
        openGui(sender, 1);
    }

    @Override
    public void openGui(Sender sender, int page) {
        Object nativeSender = sender.getNativeSender();
        if (nativeSender instanceof Player player) {
            if (page == 3) {
                openPage3(player);
            } else if (page == 2) {
                openPage2(player);
            } else {
                openPage1(player);
            }
        }
    }

    public void openPage1(Player player) {
        GrimGuiHolder holder = new GrimGuiHolder(1);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8GrimAC — Проверки (1/3)");
        holder.setInventory(inv);

        // Fill check items (page 1)
        for (CheckEntry entry : page1Checks) {
            boolean enabled = !CheckToggleRegistry.isCheckDisabled(entry.checkName());
            inv.setItem(entry.slot(), buildCheckItem(entry, enabled));
        }

        // Slot 48 (col 3) Left arrow: exits menu
        ItemStack leftArrow = new ItemStack(Material.ARROW);
        ItemMeta leftMeta = leftArrow.getItemMeta();
        if (leftMeta != null) {
            leftMeta.setDisplayName("§c← Закрыть меню");
            List<String> lore = new ArrayList<>();
            lore.add("§7Нажмите, чтобы выйти из меню настроек.");
            lore.add("§8(Альтернатива клавише ESC)");
            leftMeta.setLore(lore);
            leftArrow.setItemMeta(leftMeta);
        }
        inv.setItem(48, leftArrow);

        // Slot 50 (col 5) Right arrow: page 2
        ItemStack rightArrow = new ItemStack(Material.ARROW);
        ItemMeta rightMeta = rightArrow.getItemMeta();
        if (rightMeta != null) {
            rightMeta.setDisplayName("§eСледующая страница → (2/3)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Перейти на страницу 2 (Дополнительные проверки).");
            rightMeta.setLore(lore);
            rightArrow.setItemMeta(rightMeta);
        }
        inv.setItem(50, rightArrow);

        player.openInventory(inv);
        playClickSound(player);
    }

    public void openPage2(Player player) {
        GrimGuiHolder holder = new GrimGuiHolder(2);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8GrimAC — Дополнительные проверки (2/3)");
        holder.setInventory(inv);

        // Fill check items (page 2)
        for (CheckEntry entry : page2Checks) {
            boolean enabled = !CheckToggleRegistry.isCheckDisabled(entry.checkName());
            inv.setItem(entry.slot(), buildCheckItem(entry, enabled));
        }

        // Slot 48 (col 3) Left arrow: back to page 1
        ItemStack leftArrow = new ItemStack(Material.ARROW);
        ItemMeta leftMeta = leftArrow.getItemMeta();
        if (leftMeta != null) {
            leftMeta.setDisplayName("§e← Предыдущая страница (1/3)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Страница 1: Основные проверки");
            leftMeta.setLore(lore);
            leftArrow.setItemMeta(leftMeta);
        }
        inv.setItem(48, leftArrow);

        // Slot 50 (col 5) Right arrow: page 3
        ItemStack rightArrow = new ItemStack(Material.ARROW);
        ItemMeta rightMeta = rightArrow.getItemMeta();
        if (rightMeta != null) {
            rightMeta.setDisplayName("§eСледующая страница → (3/3)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Перейти на страницу 3 (Оптимизации).");
            rightMeta.setLore(lore);
            rightArrow.setItemMeta(rightMeta);
        }
        inv.setItem(50, rightArrow);

        player.openInventory(inv);
        playClickSound(player);
    }

    public void openPage3(Player player) {
        GrimGuiHolder holder = new GrimGuiHolder(3);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8GrimAC — Оптимизации (3/3)");
        holder.setInventory(inv);

        // Slot 20: Защита TPS (default: true)
        inv.setItem(20, buildTpsProtectionItem(PerformanceProtectionManager.isEnabled()));

        // Slot 22: Оптимизация Редстоуна (default: false)
        inv.setItem(22, buildRedstoneOptimizationItem(RedstoneOptimizer.enabled));

        // Slot 24: Оптимизация ТНТ (default: false)
        inv.setItem(24, buildTntOptimizationItem(TNTOptimizer.enabled));

        // Slot 31: Оптимизация диска и миров (default: false)
        inv.setItem(31, buildDiskOptimizationItem(DiskOptimizer.enabled));

        // Bottom row: slot 48 (col 3) Left arrow: back to page 2
        ItemStack leftArrow = new ItemStack(Material.ARROW);
        ItemMeta leftMeta = leftArrow.getItemMeta();
        if (leftMeta != null) {
            leftMeta.setDisplayName("§e← Предыдущая страница (2/3)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Страница 2: Дополнительные проверки");
            leftMeta.setLore(lore);
            leftArrow.setItemMeta(leftMeta);
        }
        inv.setItem(48, leftArrow);

        player.openInventory(inv);
        playClickSound(player);
    }

    private ItemStack buildCheckItem(CheckEntry entry, boolean enabled) {
        ItemStack item = new ItemStack(entry.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? "§a" + entry.displayName() : "§c" + entry.displayName());
            List<String> lore = new ArrayList<>();
            lore.add("§7" + entry.description());
            lore.add(" ");
            lore.add("§7Статус: " + (enabled ? "§aВКЛЮЧЕНО" : "§cВЫКЛЮЧЕНО"));
            lore.add("§eЛКМ/ПКМ: §f" + (enabled ? "Выключить проверку" : "Включить проверку"));
            meta.setLore(lore);
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildTpsProtectionItem(boolean enabled) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? "§aЗащита TPS" : "§cЗащита TPS");
            List<String> lore = new ArrayList<>();
            lore.add("§7Автоматический мониторинг нагрузки сервера в реальном времени.");
            lore.add("§7При просадке TPS ниже 15.0 временно снижает нагрузку");
            lore.add("§7тяжелых проверок для предотвращения падения производительности.");
            lore.add(" ");
            lore.add("§7Статус: " + (enabled ? "§aВКЛЮЧЕНО" : "§cВЫКЛЮЧЕНО"));
            lore.add("§eЛКМ/ПКМ: §f" + (enabled ? "Выключить защиту" : "Включить защиту"));
            meta.setLore(lore);
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildRedstoneOptimizationItem(boolean enabled) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? "§aОптимизация Редстоуна" : "§cОптимизация Редстоуна");
            List<String> lore = new ArrayList<>();
            lore.add("§7Кеширование состояния цепей и дедупликация сигналов.");
            lore.add("§7Блокирует циклические микро-осцилляции и лаг-машины,");
            lore.add("§7обеспечивая максимальный TPS без потери логики редстоуна.");
            lore.add(" ");
            lore.add("§7Статус: " + (enabled ? "§aВКЛЮЧЕНО" : "§cВЫКЛЮЧЕНО"));
            lore.add("§eЛКМ/ПКМ: §f" + (enabled ? "Выключить оптимизацию" : "Включить оптимизацию"));
            meta.setLore(lore);
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildTntOptimizationItem(boolean enabled) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? "§aОптимизация ТНТ" : "§cОптимизация ТНТ");
            List<String> lore = new ArrayList<>();
            lore.add("§7Оптимизация физики ТНТ-пушек и массовых взрывов.");
            lore.add("§7Исключает лишние 1352 лучевые проверки в воде, кеширует");
            lore.add("§7разрушение блоков и объединяет спавн дропа.");
            lore.add("§7Сохраняет 100% механику пушек, откидывание и физику.");
            lore.add(" ");
            lore.add("§7Статус: " + (enabled ? "§aВКЛЮЧЕНО" : "§cВЫКЛЮЧЕНО"));
            lore.add("§eЛКМ/ПКМ: §f" + (enabled ? "Выключить оптимизацию" : "Включить оптимизацию"));
            meta.setLore(lore);
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildDiskOptimizationItem(boolean enabled) {
        ItemStack item = new ItemStack(getSafeMaterial("LODESTONE", Material.IRON_BLOCK));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enabled ? "§aОптимизация диска и миров" : "§cОптимизация диска и миров");
            List<String> lore = new ArrayList<>();
            lore.add("§7Умное сохранение миров на диск (Smart World Save):");
            lore.add("§7Если в мире не было активности игроков за интервал");
            lore.add("§7автосохранения, запись на диск пропускается.");
            lore.add("§7При изменении хотя бы одного блока мир сохраняется как обычно.");
            lore.add("§7Фоновая дефрагментация базы данных и защита от дисковых фризов.");
            lore.add(" ");
            lore.add("§7Статус: " + (enabled ? "§aВКЛЮЧЕНО" : "§cВЫКЛЮЧЕНО"));
            lore.add("§eЛКМ/ПКМ: §f" + (enabled ? "Выключить оптимизацию" : "Включить оптимизацию"));
            meta.setLore(lore);
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GrimGuiHolder holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!player.hasPermission("grim.gui") && !player.hasPermission("grim.admin") && !player.isOp()) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (holder.getPage() == 1) {
            if (slot == 48) {
                player.closeInventory();
                playClickSound(player);
                return;
            }
            if (slot == 50) {
                openPage2(player);
                return;
            }
            for (CheckEntry entry : page1Checks) {
                if (entry.slot() == slot) {
                    handleCheckToggle(player, event.getInventory(), entry);
                    break;
                }
            }
        } else if (holder.getPage() == 2) {
            if (slot == 48) {
                openPage1(player);
                return;
            }
            if (slot == 50) {
                openPage3(player);
                return;
            }
            for (CheckEntry entry : page2Checks) {
                if (entry.slot() == slot) {
                    handleCheckToggle(player, event.getInventory(), entry);
                    break;
                }
            }
        } else if (holder.getPage() == 3) {
            if (slot == 48) {
                openPage2(player);
                return;
            }
            if (slot == 20) {
                boolean newState = !PerformanceProtectionManager.isEnabled();
                PerformanceProtectionManager.setEnabled(newState);
                event.getInventory().setItem(20, buildTpsProtectionItem(newState));
                player.sendMessage("§eGrimAC §8» §fЗащита TPS " + (newState ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА") + "§f.");
                playClickSound(player);
                saveSettings();
            } else if (slot == 22) {
                RedstoneOptimizer.enabled = !RedstoneOptimizer.enabled;
                event.getInventory().setItem(22, buildRedstoneOptimizationItem(RedstoneOptimizer.enabled));
                player.sendMessage("§eGrimAC §8» §fОптимизация Редстоуна " + (RedstoneOptimizer.enabled ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА") + "§f.");
                playClickSound(player);
                saveSettings();
            } else if (slot == 24) {
                TNTOptimizer.enabled = !TNTOptimizer.enabled;
                event.getInventory().setItem(24, buildTntOptimizationItem(TNTOptimizer.enabled));
                player.sendMessage("§eGrimAC §8» §fОптимизация ТНТ " + (TNTOptimizer.enabled ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА") + "§f.");
                playClickSound(player);
                saveSettings();
            } else if (slot == 31) {
                boolean newState = !DiskOptimizer.enabled;
                DiskOptimizer.setEnabled(newState);
                event.getInventory().setItem(31, buildDiskOptimizationItem(newState));
                player.sendMessage("§eGrimAC §8» §fОптимизация диска и миров " + (newState ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА") + "§f.");
                playClickSound(player);
                saveSettings();
            }
        }
    }

    private void handleCheckToggle(Player player, Inventory inv, CheckEntry entry) {
        if (entry.checkName().equals("ALL_CHECKS")) {
            boolean anyDisabled = !CheckToggleRegistry.getDisabledChecks().isEmpty();
            if (anyDisabled) {
                CheckToggleRegistry.clear();
                player.sendMessage("§eGrimAC §8» §aВсе проверки были включены.");
            } else {
                for (CheckEntry e : page1Checks) {
                    if (!e.checkName().equals("ALL_CHECKS")) {
                        CheckToggleRegistry.setCheckDisabled(e.checkName(), true);
                    }
                }
                for (CheckEntry e : page2Checks) {
                    if (!e.checkName().equals("ALL_CHECKS")) {
                        CheckToggleRegistry.setCheckDisabled(e.checkName(), true);
                    }
                }
                player.sendMessage("§eGrimAC §8» §cВсе проверки были выключены.");
            }
            List<CheckEntry> currentList = (inv.getHolder() instanceof GrimGuiHolder h && h.getPage() == 2) ? page2Checks : page1Checks;
            for (CheckEntry e : currentList) {
                boolean en = !CheckToggleRegistry.isCheckDisabled(e.checkName());
                inv.setItem(e.slot(), buildCheckItem(e, en));
            }
        } else {
            boolean currentlyDisabled = CheckToggleRegistry.isCheckDisabled(entry.checkName());
            boolean newDisabledState = !currentlyDisabled;
            CheckToggleRegistry.setCheckDisabled(entry.checkName(), newDisabledState);

            if (newDisabledState) {
                player.sendMessage("§eGrimAC §8» §fПроверка §b" + entry.displayName() + " §cВЫКЛЮЧЕНА§f.");
            } else {
                player.sendMessage("§eGrimAC §8» §fПроверка §b" + entry.displayName() + " §aВКЛЮЧЕНА§f.");
            }
            inv.setItem(entry.slot(), buildCheckItem(entry, !newDisabledState));
            CheckEntry allEntry = new CheckEntry(44, "ALL_CHECKS", "Все проверки", Material.EMERALD_BLOCK, "Мастер-переключатель: включить или выключить все проверки одновременно");
            inv.setItem(44, buildCheckItem(allEntry, CheckToggleRegistry.getDisabledChecks().isEmpty()));
        }
        playClickSound(player);
        saveSettings();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GrimGuiHolder) {
            event.setCancelled(true);
        }
    }

    private void playClickSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        } catch (Throwable t) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 0.7f, 1.2f);
            } catch (Throwable ignored) {}
        }
    }

    private File getSettingsFile() {
        return new File(GrimACBukkitLoaderPlugin.LOADER.getDataFolder(), "gui-settings.yml");
    }

    public void loadSettings() {
        File file = getSettingsFile();
        if (file.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                boolean tps = config.getBoolean("optimizations.tps-protection", true);
                PerformanceProtectionManager.setEnabled(tps);

                RedstoneOptimizer.enabled = config.getBoolean("optimizations.redstone-optimization", false);
                TNTOptimizer.enabled = config.getBoolean("optimizations.tnt-optimization", false);
                DiskOptimizer.setEnabled(config.getBoolean("optimizations.disk-optimization", false));

                List<String> disabled = config.getStringList("disabled-checks");
                CheckToggleRegistry.clear();
                for (String s : disabled) {
                    CheckToggleRegistry.setCheckDisabled(s, true);
                }
            } catch (Throwable t) {
                LogUtil.error("Failed to load gui-settings.yml", t);
            }
        } else {
            PerformanceProtectionManager.setEnabled(true);
            RedstoneOptimizer.enabled = false;
            TNTOptimizer.enabled = false;
            CheckToggleRegistry.clear();
        }
    }

    public void saveSettings() {
        Bukkit.getScheduler().runTaskAsynchronously(GrimACBukkitLoaderPlugin.LOADER, () -> {
            synchronized (saveLock) {
                File file = getSettingsFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("optimizations.tps-protection", PerformanceProtectionManager.isEnabled());
                config.set("optimizations.redstone-optimization", RedstoneOptimizer.enabled);
                config.set("optimizations.tnt-optimization", TNTOptimizer.enabled);
                config.set("optimizations.disk-optimization", DiskOptimizer.enabled);
                config.set("disabled-checks", new ArrayList<>(CheckToggleRegistry.getDisabledChecks()));
                try {
                    config.save(file);
                } catch (IOException e) {
                    LogUtil.error("Failed to save gui-settings.yml", e);
                }
            }
        });
    }
}
