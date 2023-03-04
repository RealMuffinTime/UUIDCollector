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
  @MethodOrder(after = "getCollection")
  @TextFieldSetting
  private final ConfigProperty<String> collectionServer = new ConfigProperty<>(
      "https://db.lilo-lookup.de/api/");
  @MethodOrder(after = "collectionServer")
  @TextFieldSetting
  private final ConfigProperty<String> authenticationKey = new ConfigProperty<>("Your AuthKey");

  @MethodOrder(after = "enabled")
  @ButtonSetting(translation = "uuidcollector.settings.uploadUUIDs.text")
  public void uploadUUIDs() {
    int uuidAmount = UUIDCollector.users.size();
    if (uuidAmount == 0) {
      Notification noUUIDsNotification = Notification.builder()
          .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
              .getIcon())
          .title(Component.text("Nothing to upload!"))
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
            .url(this.collectionServer.get() + "user/index?key=" + authenticationKey.get())
            .json(usersJson)
            .async().execute(result -> {
              if (result.getStatusCode() != 200) {
                Notification errorNotification = Notification.builder()
                    .icon(Component.icon(
                            Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
                        .getIcon())
                    .title(Component.text("Error " + result.getStatusCode()))
                    .text(Component.text("The collection server responded with an error."))
                    .duration(4500)
                    .build();

                Laby.labyAPI().minecraft().executeOnRenderThread(
                    () -> Laby.labyAPI().notificationController().push(errorNotification));
              } else {
                Notification uploadedNotification = Notification.builder()
                    .icon(Component.icon(
                            Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
                        .getIcon())
                    .title(Component.text("Uploaded UUIDs!"))
                    .text(Component.text("Uploaded " + uuidAmount + " UUIDs to the collection server."))
                    .duration(8000)
                    .build();

                Laby.labyAPI().notificationController().push(uploadedNotification);
                UUIDCollector.tempCollection.clear();
                InCollectionHUD.updateInCollection(0);
              }
            });
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    });

    uploadThread.start();
  }

  @MethodOrder(after = "uploadUUIDs")
  @ButtonSetting(translation = "uuidcollector.settings.getCollection.text")
  public void getCollection() {
    int uuidAmount = UUIDCollector.users.size();
    Notification noUUIDsNotification = Notification.builder()
        .icon(Component.icon(Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
            .getIcon())
        .title(Component.text("UUIDCollector"))
        .text(Component.text("You've collected " + uuidAmount + " UUIDs."))
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
