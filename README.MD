FrizzlenPerms

A powerful and flexible permissions & rank management plugin for Minecraft 1.21 (Paper), with built-in Discord role synchronization.
📌 Features

✅ Dynamic Rank Management – Create, modify, delete, and assign ranks.
✅ Advanced Permission System – Supports wildcards (*), negation (-), and per-world permissions.
✅ Temporary Ranks & Permissions – Auto-expiring permissions with countdown messages.
✅ Permission Auditing & Logging – Track all rank and permission changes.
✅ Customizable Permission Events – Trigger actions when permissions change.
✅ Anti-Permission Abuse System – Prevents unauthorized permission escalation.
✅ Discord Role Sync – Automatically sync ranks between Minecraft & Discord.
✅ Multi-Server Support – Works across BungeeCord & Velocity networks.
✅ GUI-Based Management – Drag-and-drop rank and permission editor.
📥 Installation

    Download the latest release from the Releases section.
    Place the .jar file into your server’s /plugins/ folder.
    Restart your server to generate the configuration files.
    Edit config.yml and discord.yml to configure the plugin.
    Run /frizzlenperms reload to apply changes.

📜 Commands & Permissions
🔹 Rank Commands
Command	Description	Permission
/frizzlenperms rank create <name> <prefix> <suffix>	Create a rank.	frizzlenperms.rank.create
/frizzlenperms rank delete <name>	Delete a rank.	frizzlenperms.rank.delete
/frizzlenperms rank set <player> <rank>	Assign a rank.	frizzlenperms.rank.set
/frizzlenperms rank history <player>	View a player's rank history.	frizzlenperms.rank.history
/frizzlenperms rank rollback <player>	Restore a previous rank.	frizzlenperms.rank.rollback
🔹 Permission Commands
Command	Description	Permission
`/frizzlenperms permission add <player	rank> <permission>`	Grant a permission.
`/frizzlenperms permission remove <player	rank> <permission>`	Remove a permission.
`/frizzlenperms permission list <player	rank>`	View all permissions.
`/frizzlenperms permission expire <player	rank> <permission> <time>`	Set a temporary permission.
🔹 Utility Commands
Command	Description	Permission
/frizzlenperms gui	Open the GUI.	frizzlenperms.gui
/frizzlenperms audit	View permission audit logs.	frizzlenperms.audit
/frizzlenperms discord sync <player>	Sync a player’s rank with Discord.	frizzlenperms.discord.sync
⚙️ Configuration
```ranks:
default:
prefix: "[Player] "
chat_color: "&7"
permissions:
- "essentials.help"
- "essentials.spawn"
moderator:
prefix: "[Mod] "
chat_color: "&a"
inherit_from: "default"
permissions:
- "essentials.kick"
- "essentials.mute"
admin:
prefix: "[Admin] "
chat_color: "&c"
inherit_from: "moderator"
permissions:
- "*"

logging:
enabled: true
log_rank_changes: true
log_permission_changes: true
log_suspicious_activity: true

anti_abuse:
enabled: true
auto_revoke_illegal_permissions: true
alert_admins_on_exploit_attempts: true
```
discord.yml
```bot_token: "YOUR_DISCORD_BOT_TOKEN"
sync:
  enabled: true
  guild_id: "123456789012345678"
  roles:
    default: "987654321098765432"
    moderator: "876543210987654321"
    admin: "765432109876543210"
  sync_on_join: true
  discord_to_minecraft: true # If true, changing a role in Discord updates Minecraft rank
```
💾 Storage Options

FrizzlenPerms supports multiple storage backends:

    SQLite (Default, lightweight, no setup required).
    MySQL (For multi-server sync).
    Redis (For real-time multi-server updates).

Edit storage.yml to configure:
```storage:
  type: "mysql" # Options: "sqlite", "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "frizzlenperms"
    user: "root"
    password: "password"
  sqlite:
    file: "plugins/FrizzlenPerms/database.db"
  redis:
    enabled: false
    host: "localhost"
    port: 6379
```
🛠️ API & Developer Hooks

FrizzlenPerms includes an API for developers to interact with the permission system.
Example usage:
```FrizzlenPermsAPI api = FrizzlenPerms.getAPI();
Rank playerRank = api.getRank(player);
boolean hasPermission = api.hasPermission(player, "essentials.fly");
```
📌 Future Plans

🚀 Web Interface for managing ranks and permissions.
🚀 More integrations (LuckPerms migration, Towny, etc.).
🚀 Economy-based ranks (Rank upgrades via in-game currency).
📜 License

This plugin is open-source under the MIT License. Feel free to contribute!
📞 Support

💬 Discord: Join our support Discord
📘 Wiki & Documentation: Coming soon
🐞 Report Issues: GitHub Issues