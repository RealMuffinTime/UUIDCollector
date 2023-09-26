package de.northernside.uuidcollector.listener;

import com.google.gson.Gson;
import de.northernside.uuidcollector.UUIDCollector;
import de.northernside.uuidcollector.hud.InCollectionHUD;
import de.northernside.uuidcollector.hud.OnServerHUD;
import java.util.UUID;
import de.northernside.uuidcollector.misc.UUIDPostRequestModel;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.io.web.request.Request;

public class ServerDisconnectListener {

  private final UUIDCollector addon;

  public ServerDisconnectListener(UUIDCollector addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent event) {
    int uuidAmount = UUIDCollector.users.size();
    if (uuidAmount == 0) {
      return;
    }

    Thread uploadThread = new Thread(() -> {
      // Allows collection size to be put back accurately, by using a temporary system while it looks up the UUIDs.
      UUIDCollector.tempCollection.putAll(UUIDCollector.users);
      UUIDCollector.users.clear();

      try {
        UUIDPostRequestModel usersRequestModel = new UUIDPostRequestModel(
            UUIDCollector.tempCollection);
        String usersJson = new Gson().toJson(usersRequestModel);
        Request.ofString()
            .url(addon.configuration().collectionServer().get() + "api/donate/" + addon.configuration().authenticationKey().get())
            .json(usersJson)
            .async().execute(result -> {
              Notification uploadedNotification = Notification.builder()
                  .icon(Component.icon(
                          Icon.texture(
                              ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
                      .getIcon())
                  .title(Component.text("Uploaded UUIDs!"))
                  .text(Component.text("Uploaded " + uuidAmount + " UUIDs to the collection server."))
                  .duration(8000)
                  .build();

              Laby.labyAPI().notificationController().push(uploadedNotification);
              UUIDCollector.tempCollection.clear();
              InCollectionHUD.updateInCollection(0);
            });
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    });

    uploadThread.start();
    try {
      uploadThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    UUIDCollector.getOnServerCollection(addon.configuration().collectionServer().get(), addon.configuration().authenticationKey().get());
  }
}

