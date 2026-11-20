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

package xyz.jonesdev.sonar.common.protocol;

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.CaptchaGenerationEndEvent;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.common.protocol.map.MapCaptchaInfo;
import xyz.jonesdev.sonar.common.protocol.map.MapColorPalette;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class CaptchaPreparer {
  private final ExecutorService PREPARATION_SERVICE = Executors.newSingleThreadExecutor();
  private final Random RANDOM = new Random();

  private volatile MapCaptchaInfo[] cached;
  private volatile int preparedAmount;

  public void prepare() {
    // Make sure we're not preparing when Sonar is already preparing answers
    if (cached != null && preparedAmount != cached.length) {
      return;
    }

    final SystemTimer timer = new SystemTimer();
    Sonar.get0().getLogger().info("Asynchronously preparing CAPTCHA answers...");
    Sonar.get0().getLogger().info("Players will be able to join even if the preparation isn't finished");

    // Prepare cache
    final int precomputeAmount = Sonar.get0().getConfig().getVerification().getMap().getPrecomputeAmount();
    final char[] alphabet = Sonar.get0().getConfig().getVerification().getMap().getAlphabet().toCharArray();

    final MapCaptchaInfo[] newCache = new MapCaptchaInfo[precomputeAmount];
    cached = newCache;
    preparedAmount = 0;

    // Prepare everything asynchronously
    PREPARATION_SERVICE.execute(() -> {
      for (int i = 0; i < precomputeAmount; i++) {
        // Generate CAPTCHA
        final char[] answer = new char[3 + RANDOM.nextInt(2)];
        for (int j = 0; j < answer.length; j++) {
          answer[j] = alphabet[RANDOM.nextInt(alphabet.length)];
        }
        final BufferedImage image = Sonar.get0().getAntiBot().getCaptchaGenerator().createImage(answer);
        // Write item into array before incrementing visible count
        newCache[i] = new MapCaptchaInfo(new String(answer), MapColorPalette.imageToBuffer(image));
        preparedAmount = i + 1;
      }

      Sonar.get0().getLogger().info("Finished preparing {} CAPTCHA answers ({}s)!", preparedAmount, timer);
      Sonar.get0().getEventManager().publish(new CaptchaGenerationEndEvent(timer, preparedAmount));
    });
  }

  public boolean isCaptchaAvailable() {
    final MapCaptchaInfo[] current = cached;
    return preparedAmount > 0 && current != null && current[0] != null;
  }

  public MapCaptchaInfo getRandomCaptcha() {
    final MapCaptchaInfo[] current = cached;
    final int count = preparedAmount;
    if (current == null || count <= 0) {
      throw new IllegalStateException("No CAPTCHA answers prepared yet");
    }
    return current[RANDOM.nextInt(Math.min(count, current.length))];
  }
}
