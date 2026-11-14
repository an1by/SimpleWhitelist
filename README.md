<a href='https://modrinth.com/plugin/simplewhitelist' target="_blank"><img alt='Modrinth' src='https://img.shields.io/modrinth/dt/mGVcEBwo?style=for-the-badge&logo=modrinth&label=Modrinth&labelColor=black&color=0037FF'/></a>

# SimpleWhitelist

Download the newest version on [Modrinth](https://modrinth.com/plugin/simplewhitelist)!

---

**SimpleWhitelist** is a lightweight, universal nickname-based whitelist plugin & mod for private Minecraft servers (`1.12 – 26.x`).

### Supported Platforms (`1.12 – 26.x` from a single branch)

| Artifact | Platforms | Minecraft Versions | Min Runtime Java |
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

### Features
- Nickname-based whitelist (`whitelist.txt`) — works seamlessly with online mode, offline mode, and **Geyser / Floodgate** Bedrock prefixes (`.`, `*`, `+`, `-`) & spaces;
- Case-sensitive or case-insensitive matching (`caseSensitive`);
- Optional automatic disconnect of non-whitelisted online players (`enforceWhitelist`);
- Configurable console notification when non-whitelisted players try to join (`logBlockedJoins`);
- Custom console commands executed on blocked join attempts (`onBlockedJoinCommands`, with `<name>` placeholder);
- Full [Adventure MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting support in `config.yml`;
- Brigadier command tree with tab-completion and granular LuckPerms / native permissions (`fabric-permissions-api-v0`, Forge/NeoForge `PermissionAPI`, Bukkit, Sponge, Velocity, BungeeCord).

### Commands
Aliases: `/simplewhitelist`, `/swl`, `/simplewl`

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/swl add <name>` | `simplewhitelist.command.add` | Add a player to the whitelist |
| `/swl remove <name>` | `simplewhitelist.command.remove` | Remove a player from the whitelist |
| `/swl list` | `simplewhitelist.command.list` | List all whitelisted players |
| `/swl enable` | `simplewhitelist.command.enable` | Enable the whitelist |
| `/swl disable` | `simplewhitelist.command.disable` | Disable the whitelist |
| `/swl reload` | `simplewhitelist.command.reload` | Reload `config.yml` and `whitelist.txt` |
| `/swl help` | Any `simplewhitelist.command.*` | Show command help |

Wildcard / base permissions: `simplewhitelist.command.*` or `simplewhitelist.command`.

### Building
Requires **Java 21** (compiles Java 8 / Java 17 targets automatically via `--release`):
```bash
./gradlew build
```
