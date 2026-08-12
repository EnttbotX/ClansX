package x.Entt.ClansX.Utils;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import x.Entt.ClansX.CX;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized manager for every player-facing message.
 * <p>
 * All text lives in {@code messages.yml}. Nothing else in the plugin should
 * hold a hardcoded string that gets shown to a player — instead, add the
 * string under a key in messages.yml and fetch it through this class.
 * <p>
 * Usage examples:
 * <pre>
 *     Messages msg = plugin.getMessages();
 *
 *     msg.send(sender, "no-permission.create");
 *     msg.send(sender, "general.not-in-clan");
 *     msg.sendList(sender, "help-menu");
 *
 *     // with placeholders (%key% -> value)
 *     msg.send(sender, "general.invites-entry", "clan", clanName);
 *
 *     // with prefix prepended automatically
 *     msg.sendPrefixed(sender, "no-permission.war");
 * </pre>
 */
public class Messages {
    private final CX plugin;
    private final FileHandler fh;

    public Messages(CX plugin, FileHandler fh) {
        this.plugin = plugin;
        this.fh = fh;
    }

    /**
     * Force a reload of messages.yml from disk (e.g. after /clansx reload).
     */
    public void reload() {
        fh.loadMsg();
    }

    private FileConfiguration cfg() {
        return fh.getMsg();
    }

    /**
     * The prefix defined at the top of messages.yml, colorized.
     */
    public String prefix() {
        return Text.color(cfg().getString("prefix", ""));
    }

    // ───────────────────────── raw retrieval ─────────────────────────

    /**
     * Raw (uncolored) string at the given dotted path, or the path itself
     * wrapped in brackets if missing, so a missing key is obvious in-game.
     */
    public String raw(String path) {
        String value = cfg().getString(path);
        return value != null ? value : "&c[Missing message: " + path + "]";
    }

    /**
     * Raw (uncolored) string list at the given dotted path.
     */
    public List<String> rawList(String path) {
        List<String> list = cfg().getStringList(path);
        return list != null ? list : new ArrayList<>();
    }

    // ───────────────────────── formatted retrieval ─────────────────────────

    /**
     * Colorized message with %key%/value replacements applied.
     * Pairs must be given as key1, value1, key2, value2, ...
     */
    public String get(String path, String... replacements) {
        return Text.color(replace(raw(path), replacements));
    }

    /**
     * Colorized message resolved with PlaceholderAPI (or the internal
     * fallback) for the given player, plus %key%/value replacements.
     */
    public String get(String path, Player player, String... replacements) {
        return Text.color(replace(raw(path), replacements), player);
    }

    /**
     * Colorized list of lines with %key%/value replacements applied.
     */
    public List<String> getList(String path, String... replacements) {
        List<String> result = new ArrayList<>();
        for (String line : rawList(path)) {
            result.add(Text.color(replace(line, replacements)));
        }
        return result;
    }

    /**
     * Colorized list of lines resolved with PlaceholderAPI for the given player.
     */
    public List<String> getList(String path, Player player, String... replacements) {
        List<String> result = new ArrayList<>();
        for (String line : rawList(path)) {
            result.add(Text.color(replace(line, replacements), player));
        }
        return result;
    }

    private String replace(String text, String... replacements) {
        if (replacements == null || replacements.length == 0) return text;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return text;
    }

    // ───────────────────────── sending helpers ─────────────────────────

    /**
     * Send a single message (no prefix) to the given sender.
     */
    public void send(CommandSender sender, String path, String... replacements) {
        sender.sendMessage(get(path, replacements));
    }

    /**
     * Send a single message (no prefix), resolving placeholders for the player.
     */
    public void send(Player player, String path, String... replacements) {
        player.sendMessage(get(path, player, replacements));
    }

    /**
     * Send a single message with the messages.yml prefix prepended.
     */
    public void sendPrefixed(CommandSender sender, String path, String... replacements) {
        sender.sendMessage(prefix() + get(path, replacements));
    }

    /**
     * Send a single message with the prefix prepended, resolving placeholders for the player.
     */
    public void sendPrefixed(Player player, String path, String... replacements) {
        player.sendMessage(prefix() + get(path, player, replacements));
    }

    /**
     * Send every line of a message list to the given sender.
     */
    public void sendList(CommandSender sender, String path, String... replacements) {
        for (String line : getList(path, replacements)) {
            sender.sendMessage(line);
        }
    }

    /**
     * Send every line of a message list, resolving placeholders for the player.
     */
    public void sendList(Player player, String path, String... replacements) {
        for (String line : getList(path, player, replacements)) {
            player.sendMessage(line);
        }
    }
}
