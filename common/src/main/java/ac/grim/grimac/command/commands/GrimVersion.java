/*
 * Copyright (C) 2020-2024 GrimAC Team
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class GrimVersion implements BuildableCommand {

    private static final String GITHUB_REPO_URL = "https://github.com/NothingFlare/Full-GrimAC";
    private static final String GITHUB_RELEASES_URL = "https://github.com/NothingFlare/Full-GrimAC/releases";
    private static final String GITHUB_RAW_VERSION_URL = "https://raw.githubusercontent.com/NothingFlare/Full-GrimAC/main/version.json";

    private static final AtomicReference<Component> updateMessage = new AtomicReference<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.of(CommonGrimArguments.URL_TIMEOUT.value(), ChronoUnit.MILLIS))
            .build();
    private static long lastCheck;

    public static void checkForUpdatesAsync(Sender sender) {
        String current = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        sender.sendMessage(Component.text()
                .append(Component.text("Full GrimAC Version: ").color(NamedTextColor.GRAY))
                .append(Component.text(current).color(NamedTextColor.AQUA))
                .build());
        // use cached message if last check was less than 1 minute ago
        final long now = System.currentTimeMillis();
        if (now - lastCheck < 60000) {
            Component message = updateMessage.get();
            if (message != null) sender.sendMessage(message);
            return;
        }
        lastCheck = now;
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(), () -> checkForUpdates(sender));
    }

    @SuppressWarnings("deprecation")
    private static void checkForUpdates(Sender sender) {
        String current = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        try {
            String apiUrl = CommonGrimArguments.API_URL.value();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .header("User-Agent", "FullGrimAC/" + current)
                    .header("Accept", "application/vnd.github.v3+json, application/json")
                    .timeout(Duration.of(CommonGrimArguments.URL_TIMEOUT.value(), ChronoUnit.MILLIS))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            // Fallback: if GitHub Releases API returns 404 (no releases published yet), check raw version.json
            if (statusCode == 404 && apiUrl.contains("api.github.com")) {
                try {
                    HttpRequest fallbackRequest = HttpRequest.newBuilder()
                            .uri(URI.create(GITHUB_RAW_VERSION_URL))
                            .GET()
                            .header("User-Agent", "FullGrimAC/" + current)
                            .timeout(Duration.of(CommonGrimArguments.URL_TIMEOUT.value(), ChronoUnit.MILLIS))
                            .build();
                    HttpResponse<String> fallbackResponse = HTTP_CLIENT.send(fallbackRequest, HttpResponse.BodyHandlers.ofString());
                    if (fallbackResponse.statusCode() >= 200 && fallbackResponse.statusCode() < 300) {
                        statusCode = fallbackResponse.statusCode();
                        responseBody = fallbackResponse.body();
                    }
                } catch (Exception ignored) {
                }
            }

            // If still 404 or empty releases, inform the user they are on the current repository version
            if (statusCode == 404) {
                Component msg = Component.text()
                        .append(Component.text("You are running Full GrimAC ").color(NamedTextColor.GREEN))
                        .append(Component.text("v" + current).color(NamedTextColor.AQUA))
                        .append(Component.text(" (Repository: ").color(NamedTextColor.GRAY))
                        .append(Component.text(GITHUB_REPO_URL).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(GITHUB_REPO_URL)))
                        .append(Component.text(")").color(NamedTextColor.GRAY))
                        .build();
                updateMessage.set(msg);
                sender.sendMessage(msg);
                return;
            }

            if (statusCode < 200 || statusCode >= 300) {
                final int finalCode = statusCode;
                Component msg = updateMessage.get();
                sender.sendMessage(Objects.requireNonNullElseGet(msg, () -> Component.text()
                        .append(MessageUtil.miniMessage("%prefix%"))
                        .append(Component.text(" Failed to check latest Full GrimAC version from GitHub. Status code: ")
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(finalCode)
                                .color(getColorForStatusCode(finalCode))
                                .decorate(TextDecoration.BOLD))
                        .build()));
                return;
            }

            JsonObject object = new JsonParser().parse(responseBody).getAsJsonObject();

            // Extract latest version tag (supports GitHub Releases "tag_name" and custom "latest_version")
            String latest = "Unknown";
            if (object.has("tag_name")) {
                String tag = object.get("tag_name").getAsString().trim();
                latest = (tag.startsWith("v") || tag.startsWith("V")) ? tag.substring(1).trim() : tag;
            } else if (object.has("latest_version")) {
                latest = object.get("latest_version").getAsString().trim();
            }

            // Extract download/release URL
            String downloadPage = GITHUB_RELEASES_URL;
            if (object.has("html_url")) {
                downloadPage = object.get("html_url").getAsString();
            } else if (object.has("download_page")) {
                downloadPage = object.get("download_page").getAsString();
            }

            @Nullable String warning = getJsonString(object, "warning", null);

            Status status;
            if (object.has("status")) {
                status = Status.getStatus(object.get("status").getAsString());
            } else {
                status = Status.SemVer.getVersionStatus(current, latest);
            }

            Component msg = switch (status) {
                case AHEAD ->
                        Component.text("You are using a development version of Full GrimAC (" + current + ")").color(NamedTextColor.LIGHT_PURPLE);
                case UPDATED ->
                        Component.text("You are using the latest version of Full GrimAC (" + current + ")").color(NamedTextColor.GREEN);
                case OUTDATED -> Component.text()
                        .append(Component.text("New Full GrimAC version found!").color(NamedTextColor.AQUA))
                        .append(Component.text(" Version ").color(NamedTextColor.GRAY))
                        .append(Component.text(latest).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                        .append(Component.text(" is available to be downloaded here: ").color(NamedTextColor.GRAY))
                        .append(Component.text(downloadPage).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(downloadPage)))
                        .build();
                case UNKNOWN ->
                        Component.text("You are using Full GrimAC version " + current).color(NamedTextColor.GREEN);
            };

            if (warning != null && !warning.isBlank()) {
                msg = msg.append(Component.text().append(Component.text(" Warning: " + warning).color(NamedTextColor.RED)).build());
            }

            updateMessage.set(msg);
            sender.sendMessage(msg);
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to check latest Full GrimAC version from GitHub.").color(NamedTextColor.RED));
            LogUtil.error("Failed to check latest Full GrimAC version from GitHub.", e);
        }
    }

    private static String getJsonString(JsonObject object, String key, String defaultValue) {
        return object.has(key) ? object.get(key).getAsString() : defaultValue;
    }

    private static NamedTextColor getColorForStatusCode(int code) {
        if (code >= 500) {
            return NamedTextColor.RED;
        } else if (code >= 400) {
            return NamedTextColor.RED;
        } else if (code >= 300) {
            return NamedTextColor.YELLOW;
        } else if (code >= 200) {
            return NamedTextColor.GREEN;
        }
        return NamedTextColor.GRAY;
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac", "fullgrimac", "fgrim")
                        .literal("version")
                        .permission("grim.version")
                        .handler(this::handleVersion)
        );
    }

    private void handleVersion(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        checkForUpdatesAsync(sender);
    }

    @AllArgsConstructor
    private enum Status {
        AHEAD("ahead"),
        UPDATED("updated"),
        OUTDATED("outdated"),
        UNKNOWN("unknown");

        private final String id;

        public static Status getStatus(String id) {
            for (Status status : Status.values()) {
                if (status.id.equals(id)) return status;
            }
            return UNKNOWN;
        }

        private static class SemVer {

            public static Status getVersionStatus(String current, String latest) {
                try {
                    var cmp = compareSemver(current, latest);
                    if (cmp == 0) {
                        return Status.UPDATED;
                    }
                    if (cmp < 0) {
                        return Status.OUTDATED;
                    }
                    return Status.AHEAD;
                } catch (Exception ignored) {}
                return Status.UNKNOWN;
            }

            public static String normalizeCoreVersion(String version) {
                String trimmed = version.trim();
                String[] dashParts = trimmed.split("-");
                String[] plusParts = dashParts[0].split("\\+");
                return plusParts[0];
            }

            public static int[] parseVersion(String version) {
                String core = normalizeCoreVersion(version);
                if (core.isEmpty()) return null;
                String[] parts = core.split("\\.");
                if (parts.length < 1) return null;

                int major = parseInt(parts[0]);
                int minor = parts.length > 1 ? parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? parseInt(parts[2]) : 0;

                if (major < 0 || minor < 0 || patch < 0) {
                    return null;
                }

                return new int[] { major, minor, patch };
            }

            private static int parseInt(String str) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }

            public static int compareSemver(String a, String b) {
                int[] pa = parseVersion(a);
                int[] pb = parseVersion(b);
                if (pa == null || pb == null) return 0;

                for (int i = 0; i < 3; i++) {
                    if (pa[i] < pb[i]) return -1;
                    if (pa[i] > pb[i]) return 1;
                }
                return 0;
            }
        }
    }
}
