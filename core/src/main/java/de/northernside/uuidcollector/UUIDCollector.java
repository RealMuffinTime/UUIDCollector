package de.northernside.uuidcollector;

import de.northernside.uuidcollector.hud.InCollectionHUD;
import de.northernside.uuidcollector.hud.OnServerHUD;
import de.northernside.uuidcollector.listener.PlayerInfoAddListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
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
    this.labyAPI().hudWidgetRegistry().register(new InCollectionHUD());
    this.labyAPI().hudWidgetRegistry().register(new OnServerHUD());
    this.logger().info("Enabled UUIDCollector Addon");

    System.out.println("000");
    Thread updateStatsThread = new Thread(() -> {
      System.out.println("well sex");
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      System.out.println("sex 2");
      Runnable toRun = () -> {
        try {
          URL url = new URL(configuration().collectionServer().get() + "key/"
              + configuration().authenticationKey().get() + "/length");
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          InputStream inputStream = connection.getInputStream();

          if (connection.getResponseCode() != 200) {
            Notification errorNotification = Notification.builder()
                .icon(Component.icon(
                        Icon.url("https://cdn.ebio.gg/logos/logo.png").aspectRatio(10, 10))
                    .getIcon())
                .title(Component.text("Error " + connection.getResponseCode()))
                .text(Component.text("The collection server responded with an error."))
                .duration(4500)
                .build();

            Laby.labyAPI().minecraft().executeOnRenderThread(
                () -> Laby.labyAPI().notificationController().push(errorNotification));
          } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null)
              response.append(inputLine);

            in.close();
            OnServerHUD.updateOnServer(Integer.parseInt(response.toString()));
          }
        } catch (IOException exception) {
          System.out.println("aaa");
          exception.printStackTrace();
        }
      };

      System.out.println("sex 222");
      scheduler.scheduleAtFixedRate(toRun, 1, 10, TimeUnit.SECONDS);
      System.out.println("666");
    });

    updateStatsThread.start();
  }

  @Override
  protected Class<UUIDCollectorConfiguration> configurationClass() {
    return UUIDCollectorConfiguration.class;
  }
}
