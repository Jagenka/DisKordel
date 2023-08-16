# install instructions

## requirements:
- Fabric Loader
- Fabric API
- Fabric Language Kotlin

([required versions](../src/main/resources/fabric.mod.json), but server log will also tell you what versions are needed if not met.)

## setup
Just put the jar file, alongside the required mods, into the mods folder. The mod will then generate a config file, where you will have to enter a Discord bot token as well as the Channel and Guild (Server) ID where all messages should be relayed to and from. (aquireable within Discord by enabling developer mode and right-clicking Server/Channel)

The bot needs to have message read/write and manage webhook permissions (permissions integer `137976138816` is the minimum, see [here](https://discord.com/developers/applications) for more info).

Check out [available commands](./commands.md) to get started.
