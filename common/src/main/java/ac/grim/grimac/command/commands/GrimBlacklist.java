/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.antibot.AntiBotManager;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GrimBlacklist implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("grim", "grimac");

        Command.Builder<Sender> bl = grimCommand
                .literal("blacklist", "bl")
                .permission("grim.blacklist");

        commandManager.command(bl.literal("clear").handler(this::handleClear));
        commandManager.command(bl.literal("list").handler(this::handleList));

        commandManager.command(bl.literal("add")
                .required("ip", StringParser.stringParser())
                .optional("duration", StringParser.stringParser())
                .handler(this::handleAdd));

        commandManager.command(bl.literal("remove")
                .required("ip", StringParser.stringParser())
                .handler(this::handleRemove));
    }

    private void handleAdd(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String ip = context.get("ip");
        String durStr = context.getOrDefault("duration", null);

        long durationMs = 600000; // 10 min default
        if (durStr != null) {
            try {
                durationMs = Long.parseLong(durStr) * 1000L;
            } catch (Throwable ignored) {}
        }

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().addBlacklist(ip, "Вручную администратором", durationMs);
            sender.sendMessage(Component.text("§eGrimAC §8» §fIP адрес §c" + ip + " §fдобавлен в черный список на §e" + (durationMs / 1000) + " сек§f."));
        }
    }

    private void handleRemove(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String ip = context.get("ip");

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().removeBlacklist(ip);
            sender.sendMessage(Component.text("§eGrimAC §8» §fIP адрес §a" + ip + " §fудален из черного списка."));
        }
    }

    private void handleClear(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().clearBlacklist();
            sender.sendMessage(Component.text("§eGrimAC §8» §aЧерный список IP адресов полностью очищен."));
        }
    }

    private void handleList(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null || manager.getDatabase() == null) return;

        Map<String, Long> blacklist = manager.getDatabase().getBlacklist();
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
        sender.sendMessage(Component.text("§e§lGrimAC — Черный список IP (§c" + blacklist.size() + "§e)"));
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : blacklist.entrySet()) {
            String ip = entry.getKey();
            long exp = entry.getValue();
            String time = (exp == -1) ? "Бессрочно" : ("Осталось: " + Math.max(0, (exp - now) / 1000) + " сек");
            sender.sendMessage(Component.text(" §8• §c" + ip + " §8| §7" + time));
        }
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
    }
}
