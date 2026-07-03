# CasualBans — MC Version Compatibility

| Range | Status | Notes |
|---|---|---|
| **MC 1.21 – 26.1.2** | ✅ **Full support** | Verified at build and runtime |
| **Paper 1.21.x** | ✅ Works | Compiles against Paper API 1.21.4 |
| **Paper 26.1.2** | ✅ Works | API-forward-compatible at runtime |

## Why it works

- **Build target:** Paper API `1.21.4-R0.1-SNAPSHOT` + Java 21
- **Runtime:** Paper 26.1.2 maintains backward API compatibility
- **`api-version: "1.21"`** tells Paper to use 1.21-era API behaviour
- **`folia-supported: true`** ensures Folia and Paper both work
- **Zero NMS/direct-implementation imports** — pure Bukkit/Paper API only
- The JAR compiled with API 1.21.4 runs **unchanged** on Paper 26.1.2

## Build

```bash
# Compile (Java 21)
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-arm64
gradle clean shadowJar

# Result: build/libs/CasualBans-1.0.0.jar
# Drop into plugins/ on Paper 1.21.x or 26.1.2
```
