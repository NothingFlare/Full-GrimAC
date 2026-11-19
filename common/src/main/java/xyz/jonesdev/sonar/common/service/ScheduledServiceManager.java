/*
 * Copyright (C) 2025 Sonar Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.jonesdev.sonar.common.service;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.update.UpdateChecker;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@UtilityClass
public final class ScheduledServiceManager {
  private ScheduledExecutorService VERBOSE;
  private ScheduledExecutorService DB_CLEANUP;
  private ScheduledExecutorService LOGIN_QUEUE;
  private ScheduledExecutorService STATISTICS;
  private ScheduledExecutorService UPDATE_NOTIFIER;

  private @NotNull ScheduledExecutorService createScheduledExecutor(final String threadName) {
    return Executors.newSingleThreadScheduledExecutor(runnable -> {
      final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
      thread.setName(threadName);
      thread.setDaemon(true);
      return thread;
    });
  }

  public synchronized void start() {
    stop();

    VERBOSE = createScheduledExecutor("sonar-verbose-thread");
    DB_CLEANUP = createScheduledExecutor("sonar-db-cleanup-thread");
    LOGIN_QUEUE = createScheduledExecutor("sonar-queue-thread");
    STATISTICS = createScheduledExecutor("sonar-statistics-thread");
    UPDATE_NOTIFIER = createScheduledExecutor("sonar-update-notifier");

    if (Sonar.get0().getVerifiedPlayerController().getCachedDatabaseType() != SonarConfiguration.Database.Type.NONE) {
      DB_CLEANUP.scheduleAtFixedRate(() -> {
        try {
          Sonar.get0().getVerifiedPlayerController().clearOld(
            Sonar.get0().getVerifiedPlayerController().getMaximumAge(), true);
        } catch (SQLException exception) {
          Sonar.get0().getLogger().error("Error cleaning database entries!");
          exception.printStackTrace(System.err);
        }
      }, Sonar.get0().getVerifiedPlayerController().getMaximumAge(), 1L, TimeUnit.DAYS);
    }

    VERBOSE.scheduleAtFixedRate(() -> {
      // Make sure to clean up the cached statistics since we don't want to display wrong values
      GlobalSonarStatistics.cleanUpCaches();
      if (Sonar.get0().getAntiBot().getBlacklist() != null) {
        Sonar.get0().getAntiBot().getBlacklist().cleanUp();
      }
      // Update the attack tracker
      Sonar.get0().getAttackTracker().checkIfUnderAttack();
      // Publish the action bar notifications
      Sonar.get0().getActionBarNotificationHandler().handleNotification();
    }, 0L, 250L, TimeUnit.MILLISECONDS);

    LOGIN_QUEUE.scheduleAtFixedRate(() -> Sonar.get0().getAntiBot().getQueue().poll(),
      1L, 1L, TimeUnit.SECONDS);

    STATISTICS.scheduleAtFixedRate(GlobalSonarStatistics::hitEverySecond,
      0L, 1L, TimeUnit.SECONDS);

    // This config setting only updates when the server is restarted
    if (Sonar.get0().getConfig().getGeneralConfig().getBoolean("general.check-for-updates")) {
      UPDATE_NOTIFIER.scheduleAtFixedRate(UpdateChecker::checkForUpdates,
        0L, 2L, TimeUnit.HOURS);
    }
  }

  public synchronized void stop() {
    if (DB_CLEANUP != null) { DB_CLEANUP.shutdownNow(); DB_CLEANUP = null; }
    if (VERBOSE != null) { VERBOSE.shutdownNow(); VERBOSE = null; }
    if (LOGIN_QUEUE != null) { LOGIN_QUEUE.shutdownNow(); LOGIN_QUEUE = null; }
    if (STATISTICS != null) { STATISTICS.shutdownNow(); STATISTICS = null; }
    if (UPDATE_NOTIFIER != null) { UPDATE_NOTIFIER.shutdownNow(); UPDATE_NOTIFIER = null; }
  }
}
