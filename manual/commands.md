# Available Commands

All commands below are Discord **slash commands**, registered in the one server (guild) configured in your config
file — they won't show up anywhere else. (DisKordel used to also support `!`-prefixed text commands; those have
been removed entirely, so a message like `!list` is now just relayed to Minecraft as regular chat, like any other
message.)

Need a reminder of what's available without leaving Discord? `/help` links back to this page.

A few commands are *universal*: they also exist as regular Minecraft commands, usable in-game by anyone with
permission to use commands. Where the in-game name differs from the Discord one, both are listed.

`<argument>` is required, `[argument]` is optional.

## Linking your Minecraft account

- `/register <minecraft_name>`: Link your Discord account to a Minecraft name. This also whitelists you.
- `/unregister`: Remove the link between your Discord account and your Minecraft name. This also removes you from
  the whitelist.

## Server status

- `/list`: List Minecraft players currently connected to the server.
- `/performance`: Show the server's current ticks per second (TPS) and milliseconds per tick (MSPT).
- `/ping`: Show the server-side ping to all currently online Minecraft players.
- `/users`: List all registered users (Discord accounts linked to a Minecraft name).

## Finding players

- `/who <part_of_name>`: Find users by part of any of their names (Discord display name, Discord username, or
  Minecraft name). *Also in-game as `/whois <partOfName>`.*
- `/where <part_of_name>`: Get the in-world coordinates of online player(s) matching part of a name. *Also in-game as
  `/whereis <partOfName>`.*

## Stats

See [Queryable Stats](./queryable_stats.md) for the full list of stat categories and identifiers.

- `/stat help`: Shows a link to the stats help page.
- `/stat get <relation> <category> <stat> [top_n] [part_of_name] [ascending]`: List players and their stat.
- `/stat compare <relation> <category> <stat> <player> [player2] [ascending]`: Compare your own (or someone else's)
  stat to another player's. If `player2` is omitted, it defaults to you.
- `/speed get [top_n] [part_of_name] [ascending]`: List players by their average speed (all `_one_cm` stats added up,
  divided by playtime).
- `/speed compare <player> [player2] [ascending]`: Compare average speed between two players. If `player2` is
  omitted, it defaults to you.
- `/playtime [part_of_name] [limit]`: List how long players have played, in total.
  *Also in-game as `/playtime [partOfName]`, always capped at 10 entries (no `limit` option in-game).*
- `/recent [days] [part_of_name] [limit] [ascending]`: List players by how much they've played in the last `days`
  days, a precise rolling window (defaults to 7). Minecraft itself only tracks a lifetime total, so this works by
  diffing periodic snapshots of that total - accurate as long as a snapshot was recently taken, which happens on
  every join/disconnect/server start-stop, and hourly while anyone is online.
- `/deaths [part_of_name] [limit]`: List how often players have died.
  *Also in-game as `/deaths [partOfName]`, always capped at 10 entries (no `limit` option in-game).*

For `get`/`list`-style commands, omitting `top_n`/`limit` lists every matching player (trimmed around your own entry
if the full list wouldn't fit in a Discord message); giving it caps the number of entries shown. `ascending` sorts
lowest-first instead of highest-first — useful for stats where a lower number is better, like deaths.

## Misc

- `/evaluate <expression> [x] [y]`: Evaluate a mathematical expression. `x` and `y` can be used as variables in the
  expression; `stack(n)`/`stacks(n)` and `chest(n)`/`chests(n)` are available as helper functions for converting item
  counts. *Also in-game as `/evaluate <expression>` (no `x`/`y` support).*

---

Every player must use `/register <minecraft_name>` to get whitelisted on the server.
