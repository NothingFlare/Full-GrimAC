/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.antibot.AltRecord;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GrimAlts implements BuildableCommand {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("grim", "grimac");

        Command.Builder<Sender> altsCommand = grimCommand
                .literal("alts", "twinks")
                .permission("grim.alts")
                .required("target", StringParser.stringParser())
                .handler(this::handleAlts);

        commandManager.command(altsCommand);
    }

    private void handleAlts(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null || manager.getDatabase() == null) {
            sender.sendMessage(Component.text("§cБаза данных твинков не инициализирована."));
            return;
        }

        List<AltRecord> alts;
        if (target.contains(".") || target.contains(":")) {
            // Target is an IP address
            alts = manager.getDatabase().getAltsByIp(target);
        } else {
            // Target is a username
            alts = manager.getDatabase().getAltsByPlayer(target);
        }

        if (alts.isEmpty()) {
            sender.sendMessage(Component.text("§eGrimAC §8» §7Для цели §b" + target + " §7не найдено зарегистрированных твинков/аккаунтов."));
            return;
        }

        String ip = alts.get(0).ip();
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
        sender.sendMessage(Component.text("§e§lGrimAC — Обнаружение твинков для: §b" + target));
        sender.sendMessage(Component.text("§7Связанный IP адрес: §f" + ip));
        sender.sendMessage(Component.text("§7Всего обнаружено аккаунтов: §c" + alts.size()));
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));

        for (AltRecord record : alts) {
            boolean isOnline = isPlayerOnline(record.username());
            String status = isOnline ? "§a[ОНЛАЙН]" : "§7[ОФФЛАЙН]";
            String firstDate = DATE_FORMAT.format(new Date(record.firstLogin()));
            String lastDate = DATE_FORMAT.format(new Date(record.lastLogin()));

            sender.sendMessage(Component.text(" §8• " + status + " §e" + record.username() +
                    " §8| §7Первый вход: §f" + firstDate + " §8| §7Посл: §f" + lastDate));
        }
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
    }

    private boolean isPlayerOnline(String username) {
        try {
            if (GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getPlayerDataManager() != null) {
                return GrimAPI.INSTANCE.getPlayerDataManager().getEntries().stream()
                        .anyMatch(p -> p.getName().equalsIgnoreCase(username));
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
