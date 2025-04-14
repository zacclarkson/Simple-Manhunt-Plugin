package com.clarkson.manhunt.commands; // Create a 'commands' package (good practice)

import com.clarkson.manhunt.Manhunt; // If needed for accessing plugin instance directly
import com.clarkson.manhunt.RoleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the /manhunt command for managing player roles.
 */
public class ManhuntCommandExecutor implements CommandExecutor, TabCompleter {

    private final RoleManager roleManager;
    // private final Manhunt plugin; // Uncomment if you need the main plugin instance

    /**
     * Constructor for the command executor.
     * @param roleManager The RoleManager instance to manage roles.
     */
    public ManhuntCommandExecutor(RoleManager roleManager) {
        this.roleManager = roleManager;
        // this.plugin = plugin; // Uncomment if needed
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check base permission
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hunter":
            case "runner":
            case "clear":
                handlePlayerRoleCommand(sender, args, subCommand);
                break;
            case "clearall":
                handleClearAllCommand(sender);
                break;
            case "list":
                handleListCommand(sender);
                break;
            default:
                sendUsage(sender, label);
                break;
        }

        return true;
    }

    private void handlePlayerRoleCommand(CommandSender sender, String[] args, String roleAction) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + args[0].toLowerCase() + " <player>").color(NamedTextColor.RED));
            return;
        }

        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(Component.text("Player '" + targetPlayerName + "' not found or not online.").color(NamedTextColor.RED));
            return;
        }

        switch (roleAction) {
            case "hunter":
                roleManager.setHunter(targetPlayer);
                sender.sendMessage(Component.text(targetPlayer.getName() + " is now a Hunter.").color(NamedTextColor.GREEN));
                // Target player already gets a message from RoleManager.setHunter
                break;
            case "runner":
                roleManager.setRunner(targetPlayer);
                sender.sendMessage(Component.text(targetPlayer.getName() + " is now the Runner.").color(NamedTextColor.GREEN));
                // Target player already gets a message from RoleManager.setRunner
                break;
            case "clear":
                roleManager.removeRole(targetPlayer);
                sender.sendMessage(Component.text(targetPlayer.getName() + "'s role has been cleared.").color(NamedTextColor.YELLOW));
                 // Target player already gets a message from RoleManager.removeRole
                break;
        }
    }

    private void handleClearAllCommand(CommandSender sender) {
        roleManager.clearRoles();
        sender.sendMessage(Component.text("All Manhunt roles have been cleared.").color(NamedTextColor.YELLOW));
        // Optionally notify all players
        // Bukkit.broadcast(Component.text("All Manhunt roles reset!").color(NamedTextColor.GOLD));
    }

     private void handleListCommand(CommandSender sender) {
        sender.sendMessage(Component.text("--- Manhunt Roles ---").color(NamedTextColor.GOLD));

        Set<UUID> hunterUUIDs = roleManager.getHunters();
        if (hunterUUIDs.isEmpty()) {
            sender.sendMessage(Component.text("Hunters: ").color(NamedTextColor.AQUA).append(Component.text("None").color(NamedTextColor.GRAY)));
        } else {
            String hunterNames = hunterUUIDs.stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()) // Use OfflinePlayer for potentially logged-out players
                .filter(name -> name != null) // Filter out potential null names if player data is weird
                .collect(Collectors.joining(", "));
            sender.sendMessage(Component.text("Hunters: ").color(NamedTextColor.AQUA).append(Component.text(hunterNames).color(NamedTextColor.WHITE)));
        }

        Set<UUID> runnerUUIDs = roleManager.getRunners();
         if (runnerUUIDs.isEmpty()) {
            sender.sendMessage(Component.text("Runners: ").color(NamedTextColor.GREEN).append(Component.text("None").color(NamedTextColor.GRAY)));
        } else {
            String runnerNames = runnerUUIDs.stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(name -> name != null)
                .collect(Collectors.joining(", "));
            sender.sendMessage(Component.text("Runners: ").color(NamedTextColor.GREEN).append(Component.text(runnerNames).color(NamedTextColor.WHITE)));
        }
         sender.sendMessage(Component.text("--------------------").color(NamedTextColor.GOLD));
    }


    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("--- Manhunt Commands ---").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " hunter <player>").color(NamedTextColor.AQUA).append(Component.text(" - Set player as Hunter.")));
        sender.sendMessage(Component.text("/" + label + " runner <player>").color(NamedTextColor.GREEN).append(Component.text(" - Set player as Runner.")));
        sender.sendMessage(Component.text("/" + label + " clear <player>").color(NamedTextColor.YELLOW).append(Component.text(" - Remove player's role.")));
        sender.sendMessage(Component.text("/" + label + " clearall").color(NamedTextColor.RED).append(Component.text(" - Remove all roles.")));
        sender.sendMessage(Component.text("/" + label + " list").color(NamedTextColor.GRAY).append(Component.text(" - List current roles.")));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> possibilities = new ArrayList<>();

        // Check permission for tab completion as well
        if (!sender.hasPermission("manhunt.admin")) {
            return completions; // Empty list
        }

        if (args.length == 1) {
            // Suggest subcommands
            possibilities.addAll(List.of("hunter", "runner", "clear", "clearall", "list"));
            String currentArg = args[0].toLowerCase();
            for (String p : possibilities) {
                if (p.startsWith(currentArg)) {
                    completions.add(p);
                }
            }
        } else if (args.length == 2) {
            // Suggest player names for relevant subcommands
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("hunter") || subCommand.equals("runner") || subCommand.equals("clear")) {
                String currentArg = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(currentArg)) {
                        // Optional: Don't suggest players who already have the target role?
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}
