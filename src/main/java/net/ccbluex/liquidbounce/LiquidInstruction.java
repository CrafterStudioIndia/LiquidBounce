/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce;

import net.ccbluex.liquidbounce.utils.client.GitInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * This class is for manual open of the jar file.
 * It should not use any other external libraries such as Kotlin stdlib, JavaFX or Minecraft
 * because they are not included in the jar.
 */
public final class LiquidInstruction {

  private LiquidInstruction() {}

  private static @NotNull List<Image> loadIcons() {
    return Stream.of(
            "/resources/liquidbounce/icon_64x64.png",
            "/resources/liquidbounce/icon_32x32.png",
            "/resources/liquidbounce/icon_16x16.png"
        ).map(LiquidInstruction.class::getResource)
        .filter(Objects::nonNull)
        .map(it -> new ImageIcon(it).getImage())
        .toList();
  }

  public static void main(String[] args) {
    JOptionPane.showMessageDialog(
        null,
        """
            Welcome to %s!

            This file is a Fabric mod, you should use it with a launcher and Fabric.
            It cannot be started by double-clicking it.
            """.formatted(LiquidBounce.CLIENT_NAME),
        LiquidBounce.CLIENT_NAME + " by " + LiquidBounce.CLIENT_AUTHOR + " (" + GitInfo.version() + ")",
        JOptionPane.INFORMATION_MESSAGE,
        loadIcons().stream().findFirst().map(ImageIcon::new).orElse(null)
    );
  }
}
