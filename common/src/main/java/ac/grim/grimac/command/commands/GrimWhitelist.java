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

import java.util.Set;

public class GrimWhitelist implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("grim", "grimac");

        Command.Builder<Sender> wl = grimCommand
                .literal("whitelist", "wl")
                .permission("grim.whitelist");

        commandManager.command(wl.literal("clear").handler(this::handleClear));
        commandManager.command(wl.literal("list").handler(this::handleList));

        commandManager.command(wl.literal("add")
                .required("target", StringParser.stringParser())
                .handler(this::handleAdd));

        commandManager.command(wl.literal("remove")
                .required("target", StringParser.stringParser())
                .handler(this::handleRemove));
    }

    private void handleAdd(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().addWhitelist(target);
            sender.sendMessage(Component.text("§eGrimAC §8» §fИгрок/IP §a" + target + " §fуспешно добавлен в белый список AntiBot."));
        }
    }

    private void handleRemove(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().removeWhitelist(target);
            sender.sendMessage(Component.text("§eGrimAC §8» §fИгрок/IP §c" + target + " §fудален из белого списка AntiBot."));
        }
    }

    private void handleClear(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getDatabase() != null) {
            manager.getDatabase().clearWhitelist();
            sender.sendMessage(Component.text("§eGrimAC §8» §aБелый список AntiBot полностью очищен."));
        }
    }

    private void handleList(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null || manager.getDatabase() == null) return;

        Set<String> whitelist = manager.getDatabase().getWhitelist();
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
        sender.sendMessage(Component.text("§e§lGrimAC — Белый список AntiBot (§a" + whitelist.size() + "§e)"));
        if (whitelist.isEmpty()) {
            sender.sendMessage(Component.text(" §8• §7Список пуст."));
        } else {
            for (String target : whitelist) {
                sender.sendMessage(Component.text(" §8• §a" + target));
            }
        }
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
    }
}
