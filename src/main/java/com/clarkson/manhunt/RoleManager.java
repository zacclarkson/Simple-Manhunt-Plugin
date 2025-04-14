// manhunt/src/main/java/com/clarkson/manhunt/RoleManager.java
package com.clarkson.manhunt;

import com.clarkson.manhunt.listeners.CompassListener; // Import CompassListener to access helper
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
// Import TextDecoration if needed elsewhere
// import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
// Other imports (UUID, Set, Collections, etc.)
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * Manages the roles (Hunter, Runner) for players in the Manhunt game.
 */
public class RoleManager {

    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();

    public enum PlayerRole { HUNTER, RUNNER, NONE }

    public void setHunter(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        runners.remove(playerUUID);
        hunters.add(playerUUID);
        player.sendMessage(Component.text("You are now a Hunter!").color(NamedTextColor.AQUA));

        // --- Give the Special Compass using the Helper Method ---
        // Ensure the player doesn't already have one before giving a new one initially
        // (The repeating task will handle replacements later)
        if (!hasTrackerCompass(player)) {
             ItemStack trackingCompass = CompassListener.createTrackerCompass(); // Call the static helper
             player.getInventory().addItem(trackingCompass);
             // Optional: Message confirming they received it?
             // player.sendMessage(Component.text("You received the Runner Tracker!").color(NamedTextColor.GOLD));
        }
        // --- End Give Compass ---
    }

    public void setRunner(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        hunters.remove(playerUUID);
        // Remove compass if they were previously a hunter and still have one
        removeTrackerCompass(player);
        runners.add(playerUUID);
        player.sendMessage(Component.text("You are now the Runner!").color(NamedTextColor.GREEN));
    }

    public void removeRole(Player player) {
        if (player == null) return;
        UUID playerUUID = player.getUniqueId();
        boolean wasHunter = hunters.remove(playerUUID);
        runners.remove(playerUUID);
        if (wasHunter) {
            // Remove compass if they are losing the hunter role
            removeTrackerCompass(player);
        }
        player.sendMessage(Component.text("Your role has been removed.").color(NamedTextColor.YELLOW));
    }

    // --- Helper method to check if a player has the compass ---
    /**
     * Checks if a player's inventory contains the special tracking compass.
     * @param player The player to check.
     * @return true if the compass is found, false otherwise.
     */
    public static boolean hasTrackerCompass(Player player) {
        if (player == null) return false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == org.bukkit.Material.COMPASS) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(CompassListener.TRACKER_COMPASS_KEY, PersistentDataType.BYTE)) {
                    return true; // Found it
                }
            }
        }
        return false; // Did not find it
    }

    // --- Helper method to remove the compass ---
    /**
     * Removes all instances of the special tracking compass from a player's inventory.
     * @param player The player whose inventory to modify.
     */
    public static void removeTrackerCompass(Player player) {
         if (player == null) return;
         player.getInventory().remove(CompassListener.createTrackerCompass()); // This might not work reliably due to meta/instance differences

         // More reliable removal: Iterate and check the tag
         ItemStack[] contents = player.getInventory().getContents();
         for (int i = 0; i < contents.length; i++) {
             ItemStack item = contents[i];
             if (item != null && item.getType() == org.bukkit.Material.COMPASS) {
                 ItemMeta meta = item.getItemMeta();
                 if (meta != null && meta.getPersistentDataContainer().has(CompassListener.TRACKER_COMPASS_KEY, PersistentDataType.BYTE)) {
                     player.getInventory().setItem(i, null); // Remove item from slot
                 }
             }
         }
         player.updateInventory(); // Update inventory visually
    }


    // --- Other methods (isHunter, isRunner, getPlayerRole, etc.) remain the same ---
    public boolean isHunter(Player player) { return player != null && hunters.contains(player.getUniqueId()); }
    public boolean isHunter(UUID uuid) { return uuid != null && hunters.contains(uuid); }
    public boolean isRunner(Player player) { return player != null && runners.contains(player.getUniqueId()); }
    public boolean isRunner(UUID uuid) { return uuid != null && runners.contains(uuid); }
    public PlayerRole getPlayerRole(Player player) { if (player == null) return PlayerRole.NONE; return getPlayerRole(player.getUniqueId()); }
    public PlayerRole getPlayerRole(UUID uuid) { if (uuid == null) return PlayerRole.NONE; if (hunters.contains(uuid)) { return PlayerRole.HUNTER; } else if (runners.contains(uuid)) { return PlayerRole.RUNNER; } else { return PlayerRole.NONE; } }
    public Set<UUID> getHunters() { return Collections.unmodifiableSet(hunters); }
    public Set<UUID> getRunners() { return Collections.unmodifiableSet(runners); }
    public void clearRoles() {
        // Before clearing roles, maybe remove compasses from all hunters?
        for (UUID hunterUUID : new HashSet<>(hunters)) { // Iterate copy to avoid concurrent modification
             Player p = org.bukkit.Bukkit.getPlayer(hunterUUID);
             if (p != null) removeTrackerCompass(p);
        }
        hunters.clear();
        runners.clear();
    }
}
