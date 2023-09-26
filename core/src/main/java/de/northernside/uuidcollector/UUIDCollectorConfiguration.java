package de.northernside.uuidcollector;

import com.google.gson.Gson;
import de.northernside.uuidcollector.hud.InCollectionHUD;
import de.northernside.uuidcollector.misc.UUIDPostRequestModel;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.MethodOrder;
import net.labymod.api.util.io.web.request.Request;

@SuppressWarnings("FieldMayBeFinal")
@ConfigName("settings")
public class UUIDCollectorConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);
  @MethodOrder(after = "getOnServer")
  @TextFieldSetting
  private final ConfigProperty<String> collectionServer = new ConfigProperty<>(
      "https://users.northernsi.de/");
  @MethodOrder(after = "collectionServer")
  @TextFieldSetting
  private final ConfigProperty<String> authenticationKey = new ConfigProperty<>("Your AuthKey");

  @MethodOrder(after = "enabled")
  @ButtonSetting(translation = "uuidcollector.settings.uploadUUIDs.text")
  public void uploadUUIDs() {
    int uuidAmount = UUIDCollector.users.size();
    if (uuidAmount == 0) {
      Notification noUUIDsNotification = Notification.builder()
          .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
              .getIcon())
          .title(Component.text("Nothing to upload!"))
          .text(Component.text("Uploaded 0 UUIDs to the collection server"))
          .duration(4000)
          .build();

      Laby.labyAPI().minecraft().executeOnRenderThread(
          () -> Laby.labyAPI().notificationController().push(noUUIDsNotification));
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
            .url(this.collectionServer.get() + "api/donate/" + authenticationKey.get())
            .json(usersJson)
            .async().execute(result -> {
                Notification uploadedNotification = Notification.builder()
                    .icon(Component.icon(
                            Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
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

    UUIDCollector.getOnServerCollection(collectionServer.get(), authenticationKey.get());
  }

  @MethodOrder(after = "uploadUUIDs")
  @ButtonSetting(translation = "uuidcollector.settings.getInCollection.text")
  public void getInCollection() {
    int uuidAmount = UUIDCollector.users.size();
    Notification noUUIDsNotification = Notification.builder()
        .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text("You've collected " + uuidAmount + " UUIDs."))
        .duration(3500)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(noUUIDsNotification));
  }

  @MethodOrder(after = "getInCollection")
  @ButtonSetting(translation = "uuidcollector.settings.getOnServer.text")
  public void getOnServer() {
    String uuidAmount = UUIDCollector.getOnServerCollection(collectionServer.get(), authenticationKey.get());;
    Notification noUUIDsNotification = Notification.builder()
        .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text("There are " + uuidAmount + " UUIDs on the server."))
        .duration(3500)
        .build();

    Laby.labyAPI().minecraft().executeOnRenderThread(
        () -> Laby.labyAPI().notificationController().push(noUUIDsNotification));
  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<String> collectionServer() {
    return this.collectionServer;
  }

  public ConfigProperty<String> authenticationKey() {
    return this.authenticationKey;
  }
}
