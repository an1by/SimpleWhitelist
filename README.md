<!-- modrinth_exclude.start -->
<a href='https://modrinth.com/plugin/simplewhitelist' target="_blank"><img alt='Modrinth' src='https://img.shields.io/modrinth/dt/mGVcEBwo?style=for-the-badge&logo=modrinth&label=Modrinth&labelColor=black&color=0037FF'/></a>

# SimpleWhitelist

Download the newest version on [Modrinth](https://modrinth.com/plugin/simplewhitelist)!

---
<!-- modrinth_exclude.end -->

# SimpleWhitelist
**SimpleWhitelist** is a lightweight, universal nickname-based whitelist plugin & mod for private Minecraft servers (**`1.12 – 26.x`**).

### Why SimpleWhitelist?
* **Nickname-based** (`whitelist.txt`) — works seamlessly on online-mode, offline-mode, proxy, and **Geyser / Floodgate** Bedrock servers (supports `.`, `*`, `+`, `-` prefixes and spaces);
* **Universal 12-platform support (`1.12 – 26.x`)** — proxies, Bukkit forks, Sponge, and mod loaders;
* **Fully customizable** [Adventure MiniMessage](https://docs.advntr.dev/minimessage/format.html) messages in `config.yml`;
* **Blocked-join logging & actions** — optional console notifications (`logBlockedJoins`) and custom console commands (`onBlockedJoinCommands`);
* **Granular permissions** — per-subcommand permissions with LuckPerms / native PermissionAPI & Brigadier tab-completion.

### Supported Platforms (`1.12 – 26.x`)

| Artifact | Platforms | Minecraft Versions | Min Java |
| :--- | :--- | :--- | :--- |
| `simplewhitelist-bukkit` | **Spigot**, **Paper**, **Purpur**, **Folia** | `1.12 – 26.x` | Java 8+ |
| `simplewhitelist-bungeecord` | **BungeeCord**, **Waterfall** | `1.12 – 26.x` | Java 8+ |
| `simplewhitelist-velocity` | **Velocity** (`3.1+`) | `1.12 – 26.x` | Java 17+ |
| `simplewhitelist-fabric` | **Fabric**, **Quilt** | `1.14 – 26.x` | Java 8+ |
| `simplewhitelist-forge-legacy` | **Forge** (Legacy FML) | `1.12 – 1.12.2` | Java 8+ |
| `simplewhitelist-forge` | **Forge** (JavaFML) | `1.13.2 – 26.x` | Java 8+ |
| `simplewhitelist-neoforge` | **NeoForge** | `1.20.2 – 26.x` | Java 17+ |
| `simplewhitelist-sponge-legacy` | **Sponge** (API 7) | `1.12 – 1.12.2` | Java 8+ |
| `simplewhitelist-sponge` | **Sponge** (API 8+) | `1.16.5 – 26.x` | Java 8+ |

### Quick Start
1. Disable vanilla whitelist in `server.properties` (`white-list=false`);
2. Put the JAR into `plugins/` (Bukkit/Sponge/Proxies) or `mods/` (Fabric/Quilt/Forge/NeoForge);
3. Start your server;
4. Add players via `/swl add <name>` and toggle via `/swl enable` / `/swl disable`.

### Commands & Permissions
Aliases: `/simplewhitelist`, `/swl`, `/simplewl`

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/swl add <name>` | `simplewhitelist.command.add` | Add a player to the whitelist |
| `/swl remove <name>` | `simplewhitelist.command.remove` | Remove a player from the whitelist |
| `/swl list` | `simplewhitelist.command.list` | Show whitelisted players |
| `/swl enable` | `simplewhitelist.command.enable` | Enable the whitelist |
| `/swl disable` | `simplewhitelist.command.disable` | Disable the whitelist |
| `/swl reload` | `simplewhitelist.command.reload` | Reload `config.yml` and `whitelist.txt` |
| `/swl help` | Any `simplewhitelist.command.*` | Show command help |

Wildcard / base permissions: `simplewhitelist.command.*` or `simplewhitelist.command`.

### Configuration (`config.yml`)
* `enabled: true` — enable/disable whitelist check;
* `caseSensitive: false` — case-sensitive nickname matching;
* `enforceWhitelist: true` — kick online non-whitelisted players on `enable` / `remove` / `reload`;
* `logBlockedJoins: true` — log blocked join attempts with username in console;
* `onBlockedJoinCommands: []` — console commands executed when a non-whitelisted player tries to join (`<name>` placeholder).
<!-- modrinth_exclude.start -->

### Building
Requires **Java 21** (compiles Java 8 / Java 17 targets automatically via `--release`):
```bash
./gradlew build
```
<!-- modrinth_exclude.end -->
