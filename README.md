# Better Server Packs (Fabric)
Better Server Packs is a server-side mod that replaces the old server-resourcepack
logic to allow changing and updating the server resourcepack while the server is running.

## What is wrong with Server Resourcepacks?
The issue Server Resourcepacks face currently is that all config about them 
(including the hash) is stored in the server.properties file. Although hashes
are technically optional for servers, not using one will cause all sorts of
weirdness when updating the resourcepack resulting in packs not updating on the client.

Using a hash on the other hand, requires a server restart for every update of the 
resourcepack and hashes are also a pain to compute. 
(Often tools will generate incorrect hashes for a file.)

## How does BSP solve this?
BSP adds the new `/pack` command to your server that allows operators 
to update the server resourcepack or push an update out to every user.

### Commands
- `/pack set [<url>] [push]`
  - This command updates the server resourcepack's URL
    and hash.
    The URL will need to be in a quoted-string.
    If you add the optional `push` argument, all online
    players will receive a resourcepack update.
- `/pack reload [push]`
  - This command forcibly reloads the resourcepack hash
    such as when you have updated the resourcepack on your
    server.
- `/pack push [<players>]`
  - Forcibly pushes a resourcepack update to the selected
    players, or all if the player argument is unspecified.



This project is a rewrite of [BetterServerPacks](https://github.com/Fisch37/better-server-resourcepack) for 1.21 Fabric servers.

I would also like to thank Max Henkel for developing the awesome [config-builder](https://github.com/henkelmax/config-builder)
library and allowing me to use it. There really aren't enough libraries for server-side configuration for Minecraft mods.
