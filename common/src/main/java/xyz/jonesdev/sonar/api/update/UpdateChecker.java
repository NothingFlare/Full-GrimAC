/*
 * Copyright (C) 2025 Sonar Contributors
 * Part of Sonar AntiBot (https://github.com/jonesdevelopment/sonar/)
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

package xyz.jonesdev.sonar.api.update;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UpdateChecker {
  @Getter
  private CheckResult lastCheckResult = CheckResult.LATEST_VERSION;

  public void checkForUpdates() {
    // Version checking disabled in Full GrimAC fork
  }

  @Getter
  @RequiredArgsConstructor
  public enum CheckResult {
    API_ERROR("api-error"),
    OUTDATED_VERSION("outdated-version"),
    UNSTABLE_VERSION("unstable-version"),
    LATEST_VERSION(null),
    UNRESOLVED(null);

    private final String configKey;
  }
}
