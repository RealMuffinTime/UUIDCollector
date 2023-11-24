// Shit Code, idc. Open up a PR if you want to clean up my code <3
// Here it is. :)
package de.northernside.uuidcollector;

import de.northernside.uuidcollector.hud.InCollectionHUD;
import de.northernside.uuidcollector.hud.OnServerHUD;
import de.northernside.uuidcollector.listener.PlayerInfoAddListener;
import de.northernside.uuidcollector.listener.ServerDisconnectListener;
import java.util.HashMap;
import java.util.UUID;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;
import net.labymod.api.notification.Notification;

@AddonMain
public class UUIDCollector extends LabyAddon<UUIDCollectorConfiguration> {

  public static HashMap<UUID, String> users = new HashMap<>();
  public static HashMap<UUID, String> tempCollection = new HashMap<>();
  public static HashMap<UUID, String> totalUsers = new HashMap<>();

  @Override
  protected void enable() {
    this.registerSettingCategory();
    this.registerListener(new PlayerInfoAddListener(this));
    this.registerListener(new ServerDisconnectListener(this));
    this.labyAPI().hudWidgetRegistry().register(new InCollectionHUD());
    this.labyAPI().hudWidgetRegistry().register(new OnServerHUD());
    this.logger().info("Enabled UUIDCollector Addon.");

    this.configuration().getOnServer();
  }

  @Override
  protected Class<UUIDCollectorConfiguration> configurationClass() {
    return UUIDCollectorConfiguration.class;
  }
}
