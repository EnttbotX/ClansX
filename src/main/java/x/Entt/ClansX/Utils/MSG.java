package x.Entt.ClansX.Utils;

import java.util.List;
import java.util.stream.Collectors;

import me.clip.placeholderapi.PlaceholderAPI;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;

public class MSG {
  public static String color(String msg) {
    if (isPlaceholderAPIAvailable())
      msg = PlaceholderAPI.setPlaceholders(null, msg); 
    return ChatColor.translateAlternateColorCodes('&', msg);
  }
  
  public static List<String> colorList(List<String> lines) {
    return lines.stream()
      .map(MSG::color)
      .collect(Collectors.toList());
  }

  public static void log(String msg) {
      Bukkit.getConsoleSender().sendMessage(color(msg));
  }
  
  private static boolean isPlaceholderAPIAvailable() {
    try {
      Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    } 
  }
}