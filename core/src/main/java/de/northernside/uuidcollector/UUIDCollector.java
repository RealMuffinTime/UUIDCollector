// Shit Code, idc. Open up a PR if you want to clean up my code <3
// Here it is. :)
package de.northernside.uuidcollector;

import com.google.gson.Gson;
import de.northernside.uuidcollector.hud.InCollectionHUD;
import de.northernside.uuidcollector.hud.OnServerHUD;
import de.northernside.uuidcollector.listener.PlayerInfoAddListener;
import de.northernside.uuidcollector.listener.ServerDisconnectListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import de.northernside.uuidcollector.misc.UUIDJsonModel;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
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

    getOnServerCollection(configuration().collectionServer().get(), configuration().authenticationKey().get());
  }

  public static Integer getOnServerCollection(String address, String key) {
    veryClassy classy = new veryClassy(address, key);
    Thread thread = new Thread(classy);
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
      return classy.getAmount();
  }



  static class veryClassy implements Runnable {
    private Integer amount = -1;
    private String address;
    private String key;

    public veryClassy(String address, String key) {
      this.address = address;
      this.key = key;
    }

    @Override
    public void run(){
      try {
        URL url = new URL( address + "api/key/" + key);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream inputStream = connection.getInputStream();
        UUIDJsonModel json  = new Gson().fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), UUIDJsonModel.class);

        if (connection.getResponseCode() != 200) {
          Notification errorNotification = Notification.builder()
              .icon(Component.icon(Icon.texture(ResourceLocation.create("uuidcollector", "textures/icon.png")).aspectRatio(10, 10))
                  .getIcon())
              .title(Component.text("Error " + connection.getResponseCode()))
              .text(Component.text("The collection server responded with an error."))
              .duration(4500)
              .build();

          Laby.labyAPI().minecraft().executeOnRenderThread(
              () -> Laby.labyAPI().notificationController().push(errorNotification));
        } else {

          amount = json.getLength();
          OnServerHUD.updateOnServer(amount);
        }
      } catch (IOException exception) {
//        UUIDCollector.this.logger().error("IOException", exception);
        exception.printStackTrace();
      }
    }

    public Integer getAmount() {
      return amount;
    }
  }

  @Override
  protected Class<UUIDCollectorConfiguration> configurationClass() {
    return UUIDCollectorConfiguration.class;
  }
}
