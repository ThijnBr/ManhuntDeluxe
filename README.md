# Better Manhunt Plugin

A Minecraft plugin for running and managing Manhunt games, where hunters try to kill runners before they can defeat the Ender Dragon.

## What is Manhunt?

Manhunt is a popular Minecraft game mode where "hunters" try to kill "runners" before the runners can beat the Ender Dragon. This plugin lets server administrators easily set up and manage Manhunt games on their servers with features like:

- **Multiple simultaneous games**: Run several manhunt games at once on your server
- **Team management**: Easily assign players to hunter or runner teams
- **Team chat**: In-game team-only communication
- **Compass tracking**: Automatically updates to track the nearest runner
- **Headstart timer**: Give runners a configurable head start before hunters are unleashed
- **World management**: Create new worlds for each game. NOTE: worlds that get generated are called manhunt_<etc>. On plugin start and end every world with this prefix gets deleted. To clean up games that have not been cleaned up the standard way.
- **Lobby system**: Central lobby for game setup and team selection

## Commands

### Main Command
- `/manhunt` - Base command with several subcommands:
  - `/manhunt create` - Opens a GUI to create a new manhunt game
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

## Configuration

The plugin creates a `config.yml` file with settings for:
- Headstart duration
- Compass update interval
- Team chat defaults
- Game end display time
- Lobby location (set via command)

This plugin is designed for server owners who want to easily set up manhunt as a minigame on their server without complex configuration or setup.
