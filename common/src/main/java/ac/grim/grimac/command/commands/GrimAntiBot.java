/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.antibot.AntiBotManager;
import ac.grim.grimac.antibot.AttackTracker;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

public class GrimAntiBot implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("grim", "grimac");

        Command.Builder<Sender> antibotBase = grimCommand
                .literal("antibot", "ab")
                .permission("grim.antibot")
                .handler(this::handleStatus);

        commandManager.command(antibotBase);

        commandManager.command(antibotBase.literal("status", "stats").handler(this::handleStatus));
        commandManager.command(antibotBase.literal("toggle").handler(this::handleToggle));
        commandManager.command(antibotBase.literal("attack").handler(this::handleAttackToggle));
        commandManager.command(antibotBase.literal("clearverified").handler(this::handleClearVerified));
        commandManager.command(antibotBase.literal("unverify")
                .required("player", StringParser.stringParser())
                .handler(this::handleUnverify));
        commandManager.command(antibotBase.literal("reload").handler(this::handleReload));
    }

    private void handleStatus(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) {
            sender.sendMessage(Component.text("§cАнтибот не инициализирован."));
            return;
        }

        AttackTracker tracker = manager.getAttackTracker();

        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
        sender.sendMessage(Component.text("§e§lGrimAC — Статус AntiBot системы"));
        sender.sendMessage(Component.text("§7Статус системы: " + (manager.getConfig().isAntibotEnabled() ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА")));
        sender.sendMessage(Component.text("§7Режим проверки: §b" + manager.getConfig().getVerificationTiming()));
        sender.sendMessage(Component.text("§7Режим атаки: " + (tracker.isUnderAttack() ? "§c§lАТАКА АКТИВНА" : "§aСпокойно")));
        sender.sendMessage(Component.text("§7Входов в секунду: §e" + tracker.getLastJoinsPerSecond() + " §7(Пик: §c" + tracker.getPeakJoinsPerSecond() + "§7)"));
        sender.sendMessage(Component.text("§7Подключений в сек: §e" + tracker.getLastConnectionsPerSecond() + " §7(Пик: §c" + tracker.getPeakConnectionsPerSecond() + "§7)"));
        sender.sendMessage(Component.text("§7Заблокировано ботов: §c" + tracker.getTotalBlockedBots().get()));
        sender.sendMessage(Component.text("§7Проверенных игроков: §a" + manager.getDatabase().getVerifiedCount()));
        sender.sendMessage(Component.text("§7IP в черном списке: §c" + manager.getDatabase().getBlacklist().size()));
        sender.sendMessage(Component.text("§7IP/Игроков в белом списке: §a" + manager.getDatabase().getWhitelist().size()));
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
    }

    private void handleToggle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        boolean newState = !manager.getConfig().isAntibotEnabled();
        manager.getConfig().setAntibotEnabled(newState);
        sender.sendMessage(Component.text("§eGrimAC §8» §fАнтибот система " + (newState ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА") + "§f. (Для сохранения измените antibot.yml)"));
    }

    private void handleAttackToggle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        boolean newAttack = !manager.getAttackTracker().isUnderAttack();
        manager.getAttackTracker().setUnderAttackManual(newAttack);
        sender.sendMessage(Component.text("§eGrimAC §8» §fТестовый режим атаки " + (newAttack ? "§cАКТИВИРОВАН" : "§aДЕАКТИВИРОВАН") + "§f."));
    }

    private void handleUnverify(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("player");
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().removeVerifiedByPlayer(target);
            sender.sendMessage(Component.text("§eGrimAC §8» §fИгрок §c" + target + " §fудален из базы проверенных игроков AntiBot."));
        }
    }

    private void handleClearVerified(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().clearVerified();
            sender.sendMessage(Component.text("§eGrimAC §8» §aБаза данных проверенных игроков AntiBot полностью очищена."));
        }
    }

    private void handleReload(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null) {
            manager.reload(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder());
            sender.sendMessage(Component.text("§eGrimAC §8» §aКонфигурация antibot.yml успешно перезагружена!"));
        }
    }
}
