# Better Manhunt Plugin

A Minecraft plugin for running and managing Manhunt games, where hunters try to kill runners before they can defeat the Ender Dragon.

## Code Structure

The codebase is organized into several packages:

- **models**: Core data structures like `Game` and `GameState` enums
- **managers**: Business logic for different aspects of the game
- **listeners**: Event handlers for player interactions
- **tasks**: Scheduled tasks for game mechanics like compass tracking
- **utils**: Utility classes for common functions

## Recent Improvements

The codebase has been improved with the following changes:

1. **Code Duplication Removed**:
   - Implemented the incomplete `updatePlayerInventory` method
   - Moved common utility methods into `GameUtils` class
   - Consolidated boss bar management through proper delegation

2. **Dead Code Eliminated**:
   - Removed unused `latestTargets` variable in `CompassTask`
   - Completed stub implementations that had only comments

3. **Improved Code Organization**:
   - Moved event handling from `HeadstartManager` to dedicated `HeadstartListener`
   - Fixed circular dependency between `GameManager` and `TaskManager`
   - Implemented proper dependency injection for `TeamChatManager`

4. **Fixed Concurrency Issues**:
   - Replaced `HashMap` with `ConcurrentHashMap` in `HeadstartManager`
   - Improved thread safety in multi-user scenarios

5. **Better Error Handling**:
   - Added proper feedback when tracking runners across different dimensions
   - Improved null checking and edge cases

## Architecture

The plugin follows a manager-based architecture:

- `GameManager`: Central coordinator for game instances
- `PlayerManager`: Handles player state and team assignments
- `GameLifecycleManager`: Controls game creation, starting, and ending
- `HeadstartManager`: Manages hunter freezing during runner headstart
- `BossBarManager`: Controls game information display in boss bars
- `TaskManager`: Coordinates scheduled tasks and acts as a facade

## Recommended Future Improvements

1. **Further Code Organization**:
   - Consider using interfaces for major managers to make testing easier
   - Break down the large `PlayerListener` into more focused listeners

2. **Dependency Injection**:
   - Consider using a lightweight DI container to manage dependencies

3. **Error Handling**:
   - Add more complete error handling and user feedback
   - Add configuration validation

4. **Testing**:
   - Add unit tests for core business logic
   - Add integration tests for event handling

5. **Configuration**:
   - Make more aspects of the game configurable
   - Add permission checks for admin commands

## Usage

Set up a Manhunt game with the following steps:

1. Create a new game with players split into hunters and runners
2. Start the game - runners get a headstart while hunters are frozen
3. Hunters track runners with special compass items
4. Game ends when all runners are defeated or the Ender Dragon is killed
