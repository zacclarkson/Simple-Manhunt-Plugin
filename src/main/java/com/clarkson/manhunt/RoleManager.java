// manhunt/src/main/java/com/clarkson/manhunt/RoleManager.java
package com.clarkson.manhunt;

import net.kyori.adventure.text.Component; // Import Adventure Component
import net.kyori.adventure.text.format.NamedTextColor; // Import Adventure NamedTextColor
import net.kyori.adventure.text.format.TextDecoration; // Import Adventure TextDecoration
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*; // Import List

/**
 * Manages the roles (Hunter, Runner) for players in the Manhunt game.
 */
public class RoleManager {

    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    public enum PlayerRole {
        HUNTER,
        RUNNER,
        NONE
    }

    public void setHunter(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        runners.remove(playerUUID);
        hunters.add(playerUUID);
        // Send message using Adventure API
        player.sendMessage(Component.text("You are now a Hunter!").color(NamedTextColor.AQUA)); // Example color

        // --- Give the Special Compass ---
        ItemStack trackingCompass = new ItemStack(Material.COMPASS);
        ItemMeta meta = trackingCompass.getItemMeta();

        if (meta != null) {
            // Set display name using Adventure Component
            meta.displayName(
                Component.text("Runner Tracker")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false) // Explicitly disable italics
            );

            // Set lore using Adventure Component list
            meta.lore(List.of( // Use List.of() for immutable list
                Component.text("Right-click to track the nearest runner!")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false) // Explicitly disable italics
            ));

            // Get the PersistentDataContainer and add the tag
            NamespacedKey key = new NamespacedKey("manhunt", "tracker_compass"); // Use the SAME key
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            trackingCompass.setItemMeta(meta); // Apply the modified meta
        }

        player.getInventory().addItem(trackingCompass);
        // --- End Give Compass ---
    }

    public void setRunner(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        hunters.remove(playerUUID);
        runners.add(playerUUID);
        // Send message using Adventure API
        player.sendMessage(Component.text("You are now the Runner!").color(NamedTextColor.GREEN)); // Example color
    }

    public void removeRole(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        hunters.remove(playerUUID);
        runners.remove(playerUUID);
        // Send message using Adventure API
        player.sendMessage(Component.text("Your role has been removed.").color(NamedTextColor.YELLOW)); // Example color
    }

    // --- Other methods (isHunter, isRunner, getPlayerRole, getHunters, getRunners, clearRoles) remain the same ---
     public boolean isHunter(Player player) {
        return player != null && hunters.contains(player.getUniqueId());
    }

    public boolean isHunter(UUID uuid) {
        return uuid != null && hunters.contains(uuid);
    }

    public boolean isRunner(Player player) {
        return player != null && runners.contains(player.getUniqueId());
    }

    public boolean isRunner(UUID uuid) {
        return uuid != null && runners.contains(uuid);
    }

    public PlayerRole getPlayerRole(Player player) {
       if (player == null) return PlayerRole.NONE;
       return getPlayerRole(player.getUniqueId());
    }

    public PlayerRole getPlayerRole(UUID uuid) {
        if (uuid == null) return PlayerRole.NONE;
        if (hunters.contains(uuid)) {
            return PlayerRole.HUNTER;
        } else if (runners.contains(uuid)) {
            return PlayerRole.RUNNER;
        } else {
            return PlayerRole.NONE;
        }
    }

    public Set<UUID> getHunters() {
        return Collections.unmodifiableSet(hunters);
    }

    public Set<UUID> getRunners() {
        return Collections.unmodifiableSet(runners);
    }

    public void clearRoles() {
        hunters.clear();
        runners.clear();
    }
}
