<a href='https://modrinth.com/plugin/simplewhitelist' target="_blank"><img alt='Modrinth' src='https://img.shields.io/modrinth/dt/mGVcEBwo?style=for-the-badge&logo=modrinth&label=Modrinth&labelColor=black&color=0037FF'/></a>
![Spigot](https://img.shields.io/badge/Spigot-orange?style=for-the-badge&logo=spigotmc)
![Fabric](https://img.shields.io/badge/Fabric-5C6BC0?style=for-the-badge&logo=fabric)
![Forge](https://img.shields.io/badge/Forge-333333?style=for-the-badge&logo=minecraft)
![Velocity](https://img.shields.io/badge/Velocity-red?style=for-the-badge&logo=velocity)
![BungeeCord](https://img.shields.io/badge/BungeeCord-00BFFF?style=for-the-badge&logo=bungee)


#### Download the newest plugin version on [Modrinth](https://modrinth.com/plugin/simplewhitelist)!

---

# ✅ SimpleWhitelist

An essential plugin for private servers that want simplicity, flexibility, and full control.  
Keep unwanted players out — with style. 😎

---

## ✨ Why SimpleWhitelist?

- 🚫 **No hassles** – players are stored by their **nicknames** (no UUIDs)!
- 🔌 **Broad support** – works with **popular server cores** (proxy & non-proxy)!
- 🎨 **Fully customizable messages** – personalize every response!
- 🧠 **User-friendly** – designed for quick setup and simple use.

---

## 🚀 Getting Started

1. ❌ **Disable** Minecraft’s default whitelist in `server.properties`: `white-list=false`
2. 📁 **Drop the plugin** into your `plugins` folder (or `mods` for mod loaders).
3. 🖥️ **Start your server.**
4. 👤 **Add yourself with:** `/swl add <player_name>`
5. 🔓 **Enable the whitelist:** `/swl enable`


---

## 🛠️ Commands

| Command                          | Description                        |
|----------------------------------|------------------------------------|
| `/swl add <player_name>`         | ➕ Add player (case-sensitive)     |
| `/swl remove <player_name>`      | ➖ Remove player                   |
| `/swl list`                      | 📜 Show all whitelisted players   |
| `/swl enable` / `/swl disable`   | 🔛 Enable / 🔴 Disable whitelist  |
| `/swl reload`                    | ♻️ Reload configuration           |

---

## 🔐 Permission

- `simplewhitelist.command` – grants access to **all commands** (Spigot/Proxy)

---

## 🔧 For Developers

## Import 👇
[![](https://jitpack.io/v/an1by/SimpleWhitelist.svg?style=flat-square)](https://jitpack.io/#an1by/SimpleWhitelist)


### Don't forget to add the dependency to your `plugin.yml` / `mods.toml` / `fabric.mod.json` / `velocity-plugin.json`

maven:
```xml
<repository>
	<id>jitpack.io</id>
	<url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>net.aniby</groupId>
    <artifactId>SimpleWhitelist</artifactId>
	<version>(release/commit)</version>
	<scope>provided</scope>
</dependency>
```
gradle:
```css
repositories {
	...
	maven { url 'https://jitpack.io' }
}
```
```css
dependencies {
	implementation 'net.aniby:SimpleWhitelist:(release/commit)'
}
```


Obtain the instance of `WhitelistHandler` like so:

```java
WhitelistHandler wh = WhitelistHandler.Api.instance;
```
and then these methods will now be accessible:
```java
/**
 * Checks whether a player is currently whitelisted.
 *
 * @param playerName the name of the player to check
 * @return {@code true} if the player is whitelisted, {@code false} otherwise
 */
boolean isWhitelisted(String playerName);

/**
 * Adds a player to the whitelist.
 *
 * @param playerName the name of the player to add
 */
void addWhitelist(String playerName);

/**
 * Removes a player from the whitelist.
 *
 * @param playerName the name of the player to remove
 */
void removeWhitelist(String playerName);

/**
 * Retrieves a list of all whitelisted players.
 *
 * @return a {@code List<String>} containing the names of all whitelisted players
 */
List<String> getWhitelisted();

/**
 * Retrieves the list of whitelisted players as a comma-separated {@code String}.
 *
 * @return a {@code String} containing all whitelisted player names separated by commas
 */
default String getWhitelistedAsString() {
    return String.join(", ", this.getWhitelisted());
}
```

---

Made with ❤️ for server owners who like it **simple**.