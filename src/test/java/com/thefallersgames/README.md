# BetterManhunt Version Compatibility Testing

This directory contains tests for the BetterManhunt plugin, including a dynamic test for Minecraft version compatibility.

## MinecraftVersionCompatibilityTest

The `MinecraftVersionCompatibilityTest` class dynamically tests the BetterManhunt plugin against multiple Minecraft versions.

### How It Works

This test:
1. Uses Maven to download Spigot API JARs for various Minecraft versions
2. Loads each version in a separate ClassLoader
3. Creates a mock Bukkit environment for each version
4. Initializes the plugin in each environment
5. Verifies the plugin loads correctly and basic functionality works

### Running the Test

Run the test using Maven:

```bash
# Download dependencies and run tests
mvn clean test
```

To run just the version compatibility test:

```bash
mvn test -Dtest=MinecraftVersionCompatibilityTest
```

### Adding a New Minecraft Version

To test with a new Minecraft version:

1. Open `pom.xml` and add a new entry to the `maven-dependency-plugin` configuration:
   ```xml
   <artifactItem>
       <groupId>org.spigotmc</groupId>
       <artifactId>spigot-api</artifactId>
       <version>1.XX.X-R0.1-SNAPSHOT</version>
       <outputDirectory>${project.basedir}/test-libs</outputDirectory>
       <destFileName>bukkit-1.XX.X-R0.1-SNAPSHOT.jar</destFileName>
   </artifactItem>
   ```
   
2. Open `MinecraftVersionCompatibilityTest.java` and add the version to the `data()` method:
   ```java
   {"1.XX.X", "1.XX.X-R0.1-SNAPSHOT"},
   ```

### Test Findings

Test results for each Minecraft version:

| Minecraft Version | Status      | Notes                                      |
|------------------|-------------|-------------------------------------------|
| 1.15.1           | Compatible  | Base version in pom.xml                    |
| 1.16.5           | Compatible  |                                           |
| 1.17.1           | Compatible  |                                           |
| 1.18.2           | Compatible  |                                           |
| 1.19.4           | Compatible  |                                           |
| 1.20.1           | Compatible  |                                           |
| 1.20.4           | Compatible  |                                           |

### Real-World Testing

While this automated test provides a good baseline for compatibility, it's still recommended to:

1. Update the Spigot API version in `pom.xml` to the target version:
   ```xml
   <dependency>
       <groupId>org.spigotmc</groupId>
       <artifactId>spigot-api</artifactId>
       <version>1.XX.X-R0.1-SNAPSHOT</version>
       <scope>provided</scope>
   </dependency>
   ```

2. Build the plugin with the target version:
   ```bash
   mvn clean package
   ```

3. Test in a real Minecraft server of that version

4. Check for any version-specific API changes that might affect plugin functionality 