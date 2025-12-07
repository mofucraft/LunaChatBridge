package com.github.lazygon.lunachatbridge.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PluginMessageListener implements Listener {

    private static final BungeeMain PLUGIN = BungeeMain.getInstance();
    private static final PluginMessageListener INSTANCE = new PluginMessageListener();
    private static final int PROTOCOL_VERSION = 2;

    static void start() {
        ProxyServer.getInstance().getPluginManager().registerListener(PLUGIN, INSTANCE);
    }

    static void stop() {
        ProxyServer.getInstance().getPluginManager().unregisterListener(INSTANCE);
    }

    private PluginMessageListener() {
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getTag().equals("lc:tobungee")) {
            return;
        }

        try {
            ByteArrayInputStream byteInStream = new ByteArrayInputStream(event.getData());
            DataInputStream in = new DataInputStream(byteInStream);

            // プロトコルバージョンを確認
            in.mark(4);
            int protocolVersion;
            try {
                protocolVersion = in.readInt();
                // v2の場合、operationを読む
                if (protocolVersion != PROTOCOL_VERSION) {
                    // 不明なバージョンの場合はリセットしてv1として処理
                    in.reset();
                    protocolVersion = 1;
                }
            } catch (Exception e) {
                in.reset();
                protocolVersion = 1;
            }

            if (protocolVersion >= 2) {
                handleChatV2(in, event);
            } else {
                handleChatV1(in, event);
            }

            in.close();
            byteInStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * v1プロトコル（後方互換）でのチャット処理
     */
    private void handleChatV1(DataInputStream in, PluginMessageEvent event) throws IOException {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOutStream);

        // 操作
        String operation = in.readUTF();
        out.writeUTF(operation);

        if (!operation.equalsIgnoreCase("chat")) {
            return;
        }

        // チャンネル名
        String channelName = in.readUTF();
        out.writeUTF(channelName);

        // プレイヤー名
        String playerName = in.readUTF();
        out.writeUTF(playerName);

        // プレイヤー表示名
        String playerDisplayName = in.readUTF();
        out.writeUTF(playerDisplayName);

        // プレイヤープレフィックス
        String playerPrefix = in.readUTF();
        out.writeUTF(playerPrefix);

        // プレイヤーサフィックス
        String playerSuffix = in.readUTF();
        out.writeUTF(playerSuffix);

        // プレイヤーのいるワールド
        String worldName = in.readUTF();
        out.writeUTF(worldName);

        // チャットメッセージ
        String chatMessage = in.readUTF();
        out.writeUTF(chatMessage);

        boolean japanize = in.readBoolean();
        out.writeBoolean(japanize);

        boolean canUseColorCode = in.readBoolean();
        out.writeBoolean(canUseColorCode);

        out.close();

        // 発言者のみが発言のプラグインメッセージを送信する。
        if (!((ProxiedPlayer) event.getReceiver()).getName().equals(playerName)) {
            return;
        }

        byte[] data = byteOutStream.toByteArray();
        relayToServers(playerName, data);
    }

    /**
     * v2プロトコルでのチャット処理
     */
    private void handleChatV2(DataInputStream in, PluginMessageEvent event) throws IOException {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOutStream);

        // プロトコルバージョン
        out.writeInt(PROTOCOL_VERSION);

        // 操作
        String operation = in.readUTF();
        out.writeUTF(operation);

        if (!operation.equalsIgnoreCase("chat")) {
            return;
        }

        // チャンネル名
        String channelName = in.readUTF();
        out.writeUTF(channelName);

        // プレイヤー名
        String playerName = in.readUTF();
        out.writeUTF(playerName);

        // プレイヤー表示名 (プレーン)
        String playerDisplayNamePlain = in.readUTF();
        out.writeUTF(playerDisplayNamePlain);

        // プレイヤー表示名 (MiniMessage) - 新規
        String playerDisplayNameMiniMessage = in.readUTF();
        out.writeUTF(playerDisplayNameMiniMessage);

        // プレイヤープレフィックス
        String playerPrefix = in.readUTF();
        out.writeUTF(playerPrefix);

        // プレイヤーサフィックス
        String playerSuffix = in.readUTF();
        out.writeUTF(playerSuffix);

        // プレイヤーのいるワールド
        String worldName = in.readUTF();
        out.writeUTF(worldName);

        // チャットメッセージ (PAPI処理済み)
        String chatMessage = in.readUTF();
        out.writeUTF(chatMessage);

        boolean japanize = in.readBoolean();
        out.writeBoolean(japanize);

        boolean canUseColorCode = in.readBoolean();
        out.writeBoolean(canUseColorCode);

        out.close();

        // 発言者のみが発言のプラグインメッセージを送信する。
        if (!((ProxiedPlayer) event.getReceiver()).getName().equals(playerName)) {
            return;
        }

        byte[] data = byteOutStream.toByteArray();
        relayToServers(playerName, data);
    }

    /**
     * 他のサーバーにメッセージをリレーする
     */
    private void relayToServers(String playerName, byte[] data) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player == null || player.getServer() == null) {
            return;
        }

        String serverFrom = player.getServer().getInfo().getName();

        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            // 送信元サーバーはスキップ
            if (serverFrom.equals(server.getName())) {
                continue;
            }

            // プレイヤーがいないサーバーはスキップ
            if (server.getPlayers().isEmpty()) {
                continue;
            }

            server.sendData("lc:tobukkit", data);
        }
    }
}
