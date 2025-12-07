# LunaChatBridge v1.2.0.1 変更履歴

## 概要

LunaChatBridge を拡張し、以下の機能を追加：
1. **shadow/outline 等のフォント装飾を保持** - MiniMessage 形式で送信
2. **PAPI プレースホルダーを送信側で処理** - 未解決プレースホルダーの除去

---

## 変更ファイル一覧

### 親 pom.xml

**ファイル:** `pom.xml`

```xml
<!-- 変更前 -->
<java.version>8</java.version>

<!-- 変更後 -->
<java.version>21</java.version>
```

**理由:** LunaChat 3.1.7 が Java 21 でコンパイルされているため

---

### Bukkit 側 pom.xml

**ファイル:** `LunaChatBridgeBukkit/pom.xml`

```xml
<!-- 変更前 -->
<repositories>
    <repository>
        <id>spigot-repo</id>
        <url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url>
    </repository>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.14.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.github.mofucraft</groupId>
        <artifactId>LunaChat</artifactId>
        <version>3.1.7</version>
    </dependency>
</dependencies>

<!-- 変更後 -->
<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.github.ucchyocean</groupId>
        <artifactId>LunaChat</artifactId>
        <version>3.1.7</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**理由:** Paper API に Adventure API が内蔵、LunaChat を mavenLocal から取得

---

### Bungee 側 pom.xml

**ファイル:** `LunaChatBridgeBungee/pom.xml`

```xml
<!-- 変更前 -->
<dependency>
    <groupId>net.md-5</groupId>
    <artifactId>bungeecord-api</artifactId>
    <version>1.14-SNAPSHOT</version>
</dependency>

<!-- 変更後 -->
<dependency>
    <groupId>net.md-5</groupId>
    <artifactId>bungeecord-api</artifactId>
    <version>1.21-R0.4</version>
    <scope>provided</scope>
</dependency>
```

---

### LunaChatListener.java

**ファイル:** `LunaChatBridgeBukkit/src/main/java/.../listener/LunaChatListener.java`

**追加内容:**

```java
// プロトコルバージョン
private static final int PROTOCOL_VERSION = 2;

// onChat() メソッド内
out.writeInt(PROTOCOL_VERSION);

// DisplayName を MiniMessage 形式で送信
Component displayNameComponent = event.getMember().getDisplayNameComponent();
String displayNameMiniMessage = MINI_MESSAGE.serialize(displayNameComponent);
out.writeUTF(displayNameMiniMessage);

// PAPI 処理済みメッセージ
String processedMessage = processPlaceholders(event);
out.writeUTF(processedMessage);
```

**新規メソッド:**
- `processPlaceholders()` - PAPI プレースホルダーを処理
- `setPlaceholders()` - リフレクションで PAPI を呼び出し
- `stripUnresolvedPlaceholders()` - 未解決プレースホルダーを除去
- `getDisplayNameComponentSafe()` - リフレクションで `getDisplayNameComponent()` を呼び出し、メソッドが存在しない場合はプレーンテキストにフォールバック

---

### PluginMessageListener.java (Bungee)

**ファイル:** `LunaChatBridgeBungee/src/main/java/.../PluginMessageListener.java`

**変更内容:**
- プロトコル v2 対応（MiniMessage フィールドの読み取り・リレー）
- v1 との後方互換性を維持

---

### 新規ファイル (Bukkit 側のみ)

| ファイル | 説明 |
|----------|------|
| `LunaChatBridgeBukkit/src/main/java/.../config/BridgeConfig.java` | 設定管理 |
| `LunaChatBridgeBukkit/src/main/resources/config.yml` | 設定ファイル |

---

## Bukkit 側設定ファイル (config.yml)

```yaml
# LunaChatBridge Bukkit Configuration

# PlaceholderAPI 設定
placeholder:
  # 未解決のプレースホルダー (%xxx%) を除去するか
  strip-unresolved: true
```

---

## ビルド方法

```bash
# 1. LunaChat を mavenLocal にインストール
cd /path/to/LunaChat
./gradlew publishToMavenLocal

# 2. LunaChatBridge をビルド
cd /path/to/LunaChatBridge
mvn clean package
```

---

## 変更を戻す場合

### 完全に戻す場合

```bash
git checkout HEAD~1 -- pom.xml
git checkout HEAD~1 -- LunaChatBridgeBukkit/pom.xml
git checkout HEAD~1 -- LunaChatBridgeBungee/pom.xml
git checkout HEAD~1 -- LunaChatBridgeBukkit/src/main/java/com/github/lazygon/lunachatbridge/bukkit/listener/LunaChatListener.java
git checkout HEAD~1 -- LunaChatBridgeBungee/src/main/java/com/github/lazygon/lunachatbridge/bungee/PluginMessageListener.java
```

### 新規ファイルを削除

```bash
rm LunaChatBridgeBukkit/src/main/java/com/github/lazygon/lunachatbridge/bukkit/config/BridgeConfig.java
rm LunaChatBridgeBukkit/src/main/resources/config.yml
```

---

## プロトコル仕様

### v2 フォーマット

```
[Protocol Version: int] = 2
[Operation: UTF] = "chat"
[Channel Name: UTF]
[Player Name: UTF]
[Display Name Plain: UTF]        // 後方互換用
[Display Name MiniMessage: UTF]  // shadow/outline 保持
[Prefix: UTF]
[Suffix: UTF]
[World Name: UTF]
[Chat Message Processed: UTF]    // PAPI 処理済み
[Japanize: boolean]
[Can Use Color Code: boolean]
```

### 後方互換性

- v1 形式のメッセージも受信可能
- 先頭バイトでバージョン判別
- LunaChat に `getDisplayNameComponent()` メソッドがない場合はプレーンテキストにフォールバック
