# Better Manhunt Plugin

A Minecraft plugin for running and managing Manhunt games, where hunters try to kill runners before they can defeat the Ender Dragon.

## What is Manhunt?

Manhunt is a popular Minecraft game mode where "hunters" try to kill "runners" before the runners can beat the Ender Dragon. This plugin lets server administrators easily set up and manage Manhunt games on their servers with features like:

- **Multiple simultaneous games**: Run several manhunt games at once on your server
- **Team management**: Easily assign players to hunter or runner teams
- **Team chat**: In-game team-only communication
- **Compass tracking**: Automatically updates to track the nearest runner with a small cooldown which can be configured.
- **Headstart timer**: Give runners a configurable head start before hunters are unleashed
- **World management**: Create new worlds for each game. NOTE: worlds that get generated are stored in a folder called ManhuntWorld. On plugin start and end, all worlds in this folder are deleted to clean up games.
- **Automatic dimension creation**: Automatically creates missing Nether and End dimensions when using current world for gameplay
- **Lobby system**: Central lobby for game setup and team selection

## Commands

### Main Command
- `/manhunt` - Base command with several subcommands:
  - `/manhunt create` - Opens a GUI to create a new manhunt game
  - `/manhunt create currentworld` - Creates a new manhunt game in your current world (admin only)
  - `/manhunt currentworld` - Alias for the above command, creates a game in the current world with auto-generation of missing Nether and End dimensions
  - `/manhunt delete <game-name>` - Deletes a manhunt game
  - `/manhunt join [game-name]` - Join a specific game or opens a GUI to select one
  - `/manhunt start [game-name]` - Starts a manhunt game
  - `/manhunt list` - Lists all active manhunt games
  - `/manhunt lobby` - Teleports you to the main lobby
  - `/manhunt setlobby` - Sets the main lobby at your current location (admin only)

### Team Commands
- `/teamhunters` - Join the hunters team in your current game
- `/teamrunners` - Join the runners team in your current game

### Game Management
- `/quitgame` - Leave your current manhunt game

### Chat Commands
- `/toall` - Toggle chat to global mode
- `/toteam` - Toggle chat to team-only mode (hunters to hunters, runners to runners)

## Code Structure

The codebase is organized into several packages:

- **models**: Core data structures like `Game` and `GameState` enums
- **managers**: Business logic for different aspects of the game
- **listeners**: Event handlers for player interactions
- **tasks**: Scheduled tasks for game mechanics like compass tracking
- **utils**: Utility classes for common functions

## Installation

1. Download the plugin jar file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Use `/manhunt setlobby` to set up your central lobby

**Note:** This plugin requires **Multiverse-Core** and has a soft dependency on **Multiverse-Inventories**. Make sure these plugins are installed on your server.

**Multiverse Limitations:**
- If Multiverse-Core is not installed, only the `/manhunt currentworld` command will be available
- Without Multiverse-Core, if there are no Nether and/or End dimensions available in your current world, the game will be cancelled with an error message

## Configuration

The plugin creates a `config.yml` file with the following configurable settings:

### Game Settings
- `headstart_seconds`: Duration in seconds that runners get before hunters are released (default: 30)
- `compass_update_interval`: How frequently the hunter's compass updates to point at the nearest runner in ticks (default: 20)
- `game_end_display_time`: How long to display game end messages and statistics in seconds (default: 10)

### Team Chat Settings
- `team_chat_by_default`: Whether team chat is enabled by default for new players (default: true)
- `team_chat_prefix`: The prefix shown for team chat messages (default: "[TEAM]")

### Lobby Settings
- `lobby.world`: The world where the main lobby is located
- `lobby.x`: X-coordinate of the lobby spawn point
- `lobby.y`: Y-coordinate of the lobby spawn point
- `lobby.z`: Z-coordinate of the lobby spawn point
- `lobby.yaw`: Horizontal rotation of players when spawned at the lobby
- `lobby.pitch`: Vertical rotation of players when spawned at the lobby

This plugin is designed for server owners who want to easily set up manhunt as a minigame on their server without complex configuration or setup.