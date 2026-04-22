# 后端 Java 运行时要求

`backend-api` 已通过 Maven Enforcer 强制要求使用 `Eclipse Temurin 17`。

## 要求

- `java.version` 必须是 `17.x`
- `java.vendor` 必须是 `Eclipse Adoptium`
- 不接受 Oracle JDK，也不接受 Oracle 发布的 OpenJDK 21/17

## 本地自检

在 `backend-api` 目录执行：

```bash
./mvnw -v
java -XshowSettings:properties -version 2>&1 | grep "java.vendor"
```

通过标准：

- `./mvnw -v` 显示 `Java version: 17.x`
- `java.vendor` 显示 `Eclipse Adoptium`

如果当前机器仍在使用 Oracle 发布的 OpenJDK，`./mvnw validate` 会直接失败，这是预期行为。

## macOS 示例

```bash
brew install --cask temurin@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

cd /Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api
./mvnw -v
```

如果机器上同时安装了多个 JDK，请把 IDE 的 Project SDK / Maven Runner JDK 也切到同一套 Temurin 17。
