package de.northernside.uuidcacher;

import de.northernside.uuidcacher.listener.PlayerInfoAddListener;
import java.util.HashMap;
import java.util.UUID;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class UUIDCacher extends LabyAddon<UUIDCacherConfiguration> {

  public static HashMap<UUID, String> users = new HashMap<>();

  @Override
  protected void enable() {
    this.registerSettingCategory();
    this.registerListener(new PlayerInfoAddListener(this));
    this.logger().info("Enabled UUIDCacher Addon");
  }

  @Override
  protected Class<UUIDCacherConfiguration> configurationClass() {
    return UUIDCacherConfiguration.class;
  }
}
