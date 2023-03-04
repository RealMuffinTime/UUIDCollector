package de.northernside.uuidcollector.hud;

import de.northernside.uuidcollector.UUIDCollector;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class InCollectionHUD extends TextHudWidget<TextHudWidgetConfig> {

  private static TextLine inCollection;

  public InCollectionHUD() {
    super("inCollection");
  }

  public static void updateInCollection(int collectionSize) {
    inCollection.updateAndFlush(collectionSize);
    inCollection.setVisible(true);
  }

  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);

    inCollection = super.createLine("In Collection", "");
    updateInCollection(UUIDCollector.users.size());
  }
}