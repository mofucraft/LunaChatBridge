package com.github.lazygon.lunachatbridge.bukkit.lc;

import com.github.ucchyocean.lc3.member.ChannelMemberOther;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChannelPlayerExtended extends ChannelMemberOther {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final String prefix;
    private final String suffix;
    private final String worldName;
    private final String displayName;
    private final String displayNameMiniMessage;
    private final boolean canUseColorCode;
    private Component displayNameComponentCache;

    public ChannelPlayerExtended(String name, String prefix, String suffix, String worldName,
                                 String displayName, String displayNameMiniMessage, boolean canUseColorCode) {
        super(name);
        this.prefix = prefix;
        this.suffix = suffix;
        this.worldName = worldName;
        this.displayName = displayName;
        this.displayNameMiniMessage = displayNameMiniMessage;
        this.canUseColorCode = canUseColorCode;
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getPlayer(getName()) != null;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    @Override
    public String getDisplayName() {
        Player player = Bukkit.getPlayer(getName());
        if (player != null) {
            // Adventure API の displayName() を使用し、レガシー形式に変換
            return LegacyComponentSerializer.legacySection().serialize(player.displayName());
        }
        return displayName;
    }

    /**
     * MiniMessage形式からComponentを取得する
     * shadow/outline等の装飾情報を保持したComponentを返す
     */
    @Override
    public Component getDisplayNameComponent() {
        // このサーバーにオンラインの場合は、Bukkitから直接取得
        Player player = Bukkit.getPlayer(getName());
        if (player != null) {
            return player.displayName();
        }

        // キャッシュがあればそれを返す
        if (displayNameComponentCache != null) {
            return displayNameComponentCache;
        }

        // MiniMessage形式からComponentを生成 (shadow等の装飾を復元)
        if (displayNameMiniMessage != null && !displayNameMiniMessage.isEmpty()) {
            try {
                displayNameComponentCache = MINI_MESSAGE.deserialize(displayNameMiniMessage);
                return displayNameComponentCache;
            } catch (Exception e) {
                // パース失敗時はプレーンテキストを返す
            }
        }

        // フォールバック: プレーンテキストからComponent
        return Component.text(displayName != null ? displayName : getName());
    }

    @Override
    public String getWorldName() {
        if (isOnline()) {
            return super.getWorldName();
        } else {
            return worldName;
        }
    }

    @Override
    public boolean hasPermission(String node) {
        if (node.equals("lunachat.allowcc")) {
            return canUseColorCode;
        }

        return super.hasPermission(node);
    }
}
