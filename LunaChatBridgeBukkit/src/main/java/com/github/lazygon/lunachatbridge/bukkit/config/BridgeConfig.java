package com.github.lazygon.lunachatbridge.bukkit.config;

import java.util.List;

/**
 * LunaChatBridge のメイン設定ファイル
 */
public class BridgeConfig extends CustomConfig {

    private static final BridgeConfig INSTANCE = new BridgeConfig("config.yml");

    private BridgeConfig(String name) {
        super(name);
    }

    public static BridgeConfig getInstance() {
        return INSTANCE;
    }

    /**
     * 未解決のプレースホルダーを除去するかどうか
     */
    public boolean isStripUnresolved() {
        return get().getBoolean("placeholder.strip-unresolved", true);
    }

    /**
     * サーバーフィルタリングが有効かどうか
     */
    public boolean isServerFilterEnabled() {
        return get().getBoolean("server-filter.enabled", false);
    }

    /**
     * ホワイトリストモードかどうか (false の場合はブラックリストモード)
     */
    public boolean isWhitelistMode() {
        return get().getBoolean("server-filter.whitelist-mode", true);
    }

    /**
     * フィルタリング対象のサーバーリスト
     */
    public List<String> getFilteredServers() {
        return get().getStringList("server-filter.servers");
    }
}
