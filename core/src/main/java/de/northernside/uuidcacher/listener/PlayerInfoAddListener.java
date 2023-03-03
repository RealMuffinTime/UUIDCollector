package de.northernside.uuidcacher.listener;

import de.northernside.uuidcacher.UUIDCacher;
import java.util.UUID;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.playerinfo.PlayerInfoAddEvent;

public class PlayerInfoAddListener {

  private final UUIDCacher addon;

  public PlayerInfoAddListener(UUIDCacher addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onPlayerInfoAdd(PlayerInfoAddEvent event) {
    UUID playerUUID = event.playerInfo().profile().getUniqueId();
    this.addon.logger().info(playerUUID.toString());
    if (playerUUID.toString().endsWith("0000-000000000000")) {
      return;
    }

    String playerUsername = event.playerInfo().profile().getUsername();
    UUIDCacher.users.put(playerUUID, playerUsername);
  }
}
