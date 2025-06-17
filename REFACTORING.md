# Better Manhunt Refactoring

This document outlines the refactoring changes made to improve the code organization and maintainability of the Better Manhunt plugin.

## Service Layer Introduction

A major refactoring was the introduction of a dedicated Service Layer to separate business logic from the manager classes. The following services were created:

### 1. LobbyService
- Centralized lobby-related functionality previously spread across multiple managers
- Handles lobby spawn management, teleportation, and player state setup
- Methods moved from:
  - LobbyManager (fully moved)
  - PlayerManager (setupLobbyPlayerState, teleportToLobbyCapsule)

### 2. GameTaskService
- Combines functionality from several task-related managers
- Consolidates boss bar management and game task scheduling
- Functionality moved from:
  - TaskManager
  - GameTaskManager
  - BossBarManager

## Deprecated Managers

The following managers were deprecated in favor of the service layer:

1. **LobbyManager** - Now delegates to LobbyService
2. **TaskManager** - Now delegates to GameTaskService
3. **BossBarManager** - Functionality moved to GameTaskService
4. **GameTaskManager** - Functionality moved to GameTaskService

## Class Relationship Improvements

- Circular dependencies were reduced through the use of supplier functions
- Related functionality was grouped into cohesive service classes
- Class responsibilities are better defined and more focused

## Next Steps

The following could be considered for future refactoring:

1. Further consolidate GameLifecycleManager functionality into a dedicated GameLifecycleService
2. Refactor PlayerManager to move state management into a dedicated PlayerService
3. Update all references to deprecated managers to use the services directly
4. Consider transitioning from the Manager pattern to a more functional approach
5. Implement a proper dependency injection mechanism to further reduce coupling

## Benefits

- Improved code organization with clear separation of concerns
- Reduced code duplication
- More maintainable and testable architecture
- Better encapsulation of related functionality
- Simplified class relationships 