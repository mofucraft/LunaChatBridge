package com.github.lazygon.lunachatbridge.bukkit.listener;

import com.github.lazygon.lunachatbridge.bukkit.config.BungeeChannels;
import com.github.lazygon.lunachatbridge.bukkit.lc.ChannelPlayerExtended;
import com.github.lazygon.lunachatbridge.bukkit.lc.DataMapsExtended;
import com.github.ucchyocean.lc3.LunaChat;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.Messages;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.member.ChannelMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PluginMessages implements PluginMessageListener {

    private static final PluginMessages INSTANCE = new PluginMessages();
    private List<String> players = new ArrayList<>();

    private PluginMessages() {
    }

    public static PluginMessages getInstance() {
        return INSTANCE;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("lc:tobukkit")) {
            return;
        }

        try {
            ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(message);
            DataInputStream in = new DataInputStream(byteArrayIn);

            String operation = in.readUTF();
            if (operation.equalsIgnoreCase("chat")) {
                String channelName = in.readUTF();
                String playerName = in.readUTF();
                String playerDisplayName = in.readUTF();
                String playerPrefix = in.readUTF();
                String playerSuffix = in.readUTF();
                String worldName = in.readUTF();
                String chatMessage = in.readUTF();
                boolean japanize = in.readBoolean();
                boolean canUseColorCode = in.readBoolean();

                in.close();
                byteArrayIn.close();

                boolean defaultJapanize = LunaChat.getAPI().isPlayerJapanize(playerName);
                if (japanize != defaultJapanize) {
                    LunaChat.getAPI().setPlayersJapanize(playerName, japanize);
                }

                // プライベートメッセージ
                if (channelName.contains(">")) {
                    if (Bukkit.getPlayer(playerName) != null) {
                        return;
                    }
                    String invited = channelName.substring(channelName.indexOf(">") + 1);
                    if (Bukkit.getPlayer(invited) == null) {
                        return;
                    }
                    ChannelMember channelPlayer = new ChannelPlayerExtended(playerName, playerPrefix, playerSuffix,
                            worldName, playerDisplayName, canUseColorCode);
                    sendTellMessage(channelPlayer, invited, chatMessage);
                    return;
                }

                if (!BungeeChannels.getInstance().isBungeeChannel(channelName)) {
                    return;
                }

                ChannelMember channelPlayer = new ChannelPlayerExtended(playerName, playerPrefix, playerSuffix,
                        worldName, playerDisplayName, canUseColorCode);
                Channel lcChannel = LunaChat.getAPI().getChannel(channelName);
                if (lcChannel != null) {
                    lcChannel.chat(channelPlayer, chatMessage);
                }

                if (japanize != defaultJapanize) {
                    LunaChat.getAPI().setPlayersJapanize(playerName, defaultJapanize);
                }

            } else if (operation.equalsIgnoreCase("updateplayers")) {
                players = new ArrayList<>(Arrays.asList(in.readUTF().split(",", -1)));
            } else if (operation.equalsIgnoreCase("joinplayer")) {
                String playerName = in.readUTF();
                if (!players.contains(playerName)) {
                    players.add(playerName);
                }
            } else if (operation.equalsIgnoreCase("disconnectplayer")) {
                players.remove(in.readUTF());
            }

            in.close();
            byteArrayIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getBungeePlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Tellコマンドの実行処理を行う
     *
     * @param inviter
     * @param invitedName
     * @param message
     * @author ucchy
     */
    protected void sendTellMessage(ChannelMember inviter, String invitedName, String message) {
        ChannelMember invited = ChannelMember.getChannelMember(invitedName);

        // 招待相手が自分自身でないか確認する
        if (inviter.getName().equals(invited.getName())) {
            inviter.sendMessage(Messages.errmsgCannotSendPMSelf());
            return;
        }

        // チャンネルが存在するかどうかをチェックする
        LunaChatAPI api = LunaChat.getAPI();
        String cname = inviter.getName() + ">" + invited.getName();
        Channel channel = api.getChannel(cname);
        if (channel == null) {
            // チャンネルを作成して、送信者、受信者をメンバーにする
            channel = api.createChannel(cname);
            channel.setVisible(false);
            channel.addMember(inviter);
            channel.addMember(invited);
            channel.setPrivateMessageTo(ChannelMember.getChannelMember(invited.getName()));

        }

        // メッセージがあるなら送信する
        if (message.trim().length() > 0) {
            channel.chat(inviter, message);
        }

        // 送信履歴を残す
        DataMapsExtended.putIntoPMMap(invited.getName(), inviter.getName());
        DataMapsExtended.putIntoPMMap(inviter.getName(), invited.getName());
    }
}
