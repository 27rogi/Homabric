<img src="https://raw.githubusercontent.com/rogi27/Homabric/main/src/main/resources/assets/homabric/icon.png" height="128" />

# 🏠 Homabric
> ⚠️This mod is in early stage of development, you can use all functions that are listed below, but they may be changed in future. Code quality might be very poor sometimes and it will be improved in future.

Little yet powerful home management mod for Fabric.

This mod can be used on server or client, it provides commands for home management with support for:
- ⭐ Sharing homes with players.
- ⭐ Using GUI for teleportation and viewing all home information.
- ⭐ Editing home icons for GUI with support for modded items.
- ⭐ Ability to easily configure the mod and view player locations.

### Languages
- 🇬🇧 English
- 🇷🇺 Russian

## Commands
### Players
- /h **OR** /home `homabric.base.use`
- /h <name?> `homabric.base.byName`
- /h set <name?> `homabric.base.set`
- /h remove <home> `homabric.base.remove`
- /h p <player> <home> `homabric.base.others`
- /h list `homabric.base.list`
- /h allow <player> <home> `homabric.base.allow`
- /h disallow <player> <home> `homabric.base.disallow`
- /h setIcon <home> <item identificator> `homabric.base.setIcon`
### Admins
You can access to admin commands by using `/homabric`.
- /homabric `homabric.admin.use`
- /homabric reload `homabric.admin.reload`
- /homabric teleport <player> <home> `homabric.admin.teleport`
- /homabric set <player> <home> `homabric.admin.set`
- /homabric remove <player> <home> `homabric.admin.remove`
- /homabric list <player> `homabric.admin.list`
### Legacy
You can disable these commands in configuration file by setting `enableOldschoolCommands` to `false`.
- /sethome <name> `homabric.base.set`
- /removehome <home> `homabric.base.remove`
- /playerhome <player> <home> `homabric.base.others`
- /listhome `homabric.base.list`
- /allowhome <player> <home> `homabric.base.allow`
- /disallowhome <player> <home> `homabric.base.disallow`

## Configuration
I developed this mod with simplicity in mind, I decided to store playerdata inside configuration file instead of NBT in Entity, this allows server owners fast migration between maps and provides them all information about players.

```shell
config {
    # Do not touch this value, it allows mod to 
    # check if config file is outdated or not
    configVersion=1
    # This option enables alternative command variants like /sethome, /removehome and etc.
    enableOldschoolCommands=true
    # Sets the maximum amount of homes per player
    maxHomes=2
    # List of players with their homes
    players {
        # Player nickname (not a DisplayName)
        ExamplePlayer {
            # List of player homes
            homes {
                # Name of the home
                "berries" {
                    # Players that can access this home
                    allowed-players=[]
                    # Icon must be an identifier, for example 'minecraft:cobblestone'
                    icon="minecraft:map"
                    # World where home is located
                    world="minecraft:overworld"
                    # Coordinates, including head position
                    x=160.66
                    y=72.0
                    yaw=-51.45
                    z=-76.45
                }
                # Example of home with allowed player 'Rogi27'
                home {
                    allowed-players=[
                        Rogi27
                    ]
                    icon="minecraft:map"
                    pitch=8.25
                    world="minecraft:overworld"
                    x=1337.3
                    y=172.0
                    yaw=123.1
                    z=276.45
                }
            }
        }
    }
}
```

## Planned features
- [ ] Ability to define `/home` command aliases in config.
- [ ] Home limit can be set via permissions.
- [ ] Code quality improvements.