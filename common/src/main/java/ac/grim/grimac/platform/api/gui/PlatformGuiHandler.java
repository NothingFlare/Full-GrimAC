/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.api.gui;

import ac.grim.grimac.platform.api.sender.Sender;

public interface PlatformGuiHandler {
    void openGui(Sender sender);
    void openGui(Sender sender, int page);
}
