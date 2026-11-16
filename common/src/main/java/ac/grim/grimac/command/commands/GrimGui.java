/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.command.requirements.PlayerSenderRequirement;
import ac.grim.grimac.platform.api.gui.PlatformGuiHandler;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class GrimGui implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("grim", "grimac");

        Command.Builder<Sender> guiCommand = grimCommand
                .literal("gui", "menu")
                .permission("grim.gui")
                .handler(this::handleGui)
                .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.INSTANCE));

        commandManager.command(guiCommand);
    }

    private void handleGui(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlatformGuiHandler guiHandler = GrimAPI.INSTANCE.getGuiHandler();
        if (guiHandler != null) {
            guiHandler.openGui(sender);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "gui-not-supported", "%prefix% &cGUI is not supported on this platform!"));
        }
    }
}
