# Manhunt Plugin

## Description

A custom Minecraft plugin for the Paper server platform (API 1.21+) that recreates the popular Manhunt game mode. Hunters are given a special compass to track down Runners across dimensions.

## Features

* **Hunter/Runner Roles:** Assign players distinct roles.
* **Tracking Compass:**
    * Hunters receive a special, enchanted compass named "Runner Tracker".
    * Compass identifies itself via a unique tag (prevents using regular compasses).
    * Right-clicking updates the compass target.
* **Advanced Tracking Logic:**
    * Tracks the nearest Runner in the **same dimension**.
    * Tracks the **last known location** of the nearest Runner if they are in a different dimension (updates when Runners change worlds).
    * Provides feedback messages indicating *who* is being tracked (directly or indirectly), without revealing distance.
* **Compass Persistence:** Automatically checks Hunter inventories periodically (every 10 seconds) and replaces the tracking compass if it's missing.
* **Role Management Commands:**
    * `/manhunt` (alias `/mh`) command for managing roles.
    * Subcommands: `hunter`, `runner`, `clear`, `clearall`, `list`.
    * Tab completion for subcommands and online player names.
* **Permissions:** Command usage restricted by the `manhunt.admin` permission.
* **Paper Compatibility:** Uses programmatic command registration suitable for Paper servers.

## Requirements

* **Server:** PaperMC (or a compatible fork like Purpur) running Minecraft 1.21.x.
* **Java:** Java 21 or higher is required to build the plugin (as specified in `pom.xml`). The server itself might run on a lower Java version compatible with MC 1.21.

## Installation

1.  Build the plugin using Maven (see below) or download a pre-built `.jar` file.
2.  Place the `manhunt-1.0-SNAPSHOT.jar` file into your server's `plugins/` directory.
3.  Restart or reload your server. (Restart is generally recommended after adding new plugins).

## Usage

### Commands

The base command is `/manhunt` or `/mh`. Requires the `manhunt.admin` permission.

* `/manhunt hunter <player>`: Sets the specified online player as a Hunter. Gives them the tracking compass.
* `/manhunt runner <player>`: Sets the specified online player as a Runner. Removes compass if they were a Hunter.
* `/manhunt clear <player>`: Removes any Manhunt role from the specified online player. Removes compass if they were a Hunter.
* `/manhunt clearall`: Removes all Manhunt roles (Hunter/Runner) from all players known to the plugin. Removes compasses from former Hunters.
* `/manhunt list`: Displays a list of current Hunters and Runners (includes offline players who still have roles).

### Permissions

* `manhunt.admin`: Grants access to all `/manhunt` commands. Assign this permission using your server's permission plugin (e.g., LuckPerms).

## Building from Source

1.  Ensure you have **Java 21 (JDK)** and **Apache Maven** installed.
2.  Clone the repository or download the source code.
3.  Navigate to the project's root directory (where `pom.xml` is located) in your terminal or command prompt.
4.  Run the Maven command: `mvn clean package`
5.  The compiled plugin JAR (`manhunt-1.0-SNAPSHOT.jar`) will be located in the `target/` directory.

## Future Ideas / To-Do (Optional)

* Implement game start/stop logic.
* Add configuration options (e.g., compass check interval, messages).
* Handle cases where a Hunter's inventory is full when giving the compass.
* Add support for compass tracking in the offhand.
* Consider persistence for roles/locations across server restarts.
