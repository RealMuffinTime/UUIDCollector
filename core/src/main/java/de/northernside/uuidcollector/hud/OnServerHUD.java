package de.northernside.uuidcollector.hud;

import de.northernside.uuidcollector.UUIDCollector;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class OnServerHUD extends TextHudWidget<TextHudWidgetConfig> {

  private static TextLine onServer;

  public OnServerHUD() {
    super("onServer");
  }

  public static void updateOnServer(int serverSize) {
    onServer.updateAndFlush(serverSize);
    onServer.setVisible(true);
  }

  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);

    onServer = super.createLine("On Server", "");
    updateOnServer(UUIDCollector.users.size());
  }
}