package com.cecer1.hypixelutils;

import com.cecer1.hypixelutils.clientcommands.LiteLoaderCommandRegister;
import com.cecer1.hypixelutils.events.EventManager;
import com.cecer1.hypixelutils.events.adapters.*;
import com.cecer1.hypixelutils.utils.MD5Checksum;
import com.cecer1.hypixelutils.utils.WorldDimension;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderEventBroker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.util.IChatComponent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LiteModHypixelUtils implements ChatFilter, Tickable, OutboundChatFilter, JoinGameListener, PacketHandler {
    public static LiteModHypixelUtils instance;

    private LiteLoaderOnBungeeServerChangeAdapter liteloaderOnBungeeServerChangeAdapter;
    private LiteLoaderOnChatAdapter liteloaderOnChatAdapter;
    private LiteLoaderOnTickAdapter liteloaderOnTickAdapter;
    private LiteLoaderOnRenderAdapter liteLoaderOnRenderAdapter;
    private LiteLoaderOnConnectAdapter liteLoaderOnConnectAdapter;

    private LiteLoaderCommandRegister liteloaderCommandRegister;

    public LiteModHypixelUtils() {
        instance = this;
    }

    @Override
    public boolean onChat(IChatComponent chat, String message, LiteLoaderEventBroker.ReturnValue<IChatComponent> newMessage) {
        return liteloaderOnChatAdapter.trigger(chat, message, newMessage);
    }

    @Override
    public boolean onSendChatMessage(String message) {
        if(HypixelUtilsCore.skipEventProcessingChatMessages.contains(message)) {
            HypixelUtilsCore.skipEventProcessingChatMessages.remove(message);
            return true;
        }
        return liteloaderCommandRegister.trigger(message);
    }

    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {
        if(liteloaderOnTickAdapter != null)
            liteloaderOnTickAdapter.trigger(minecraft, partialTicks, inGame, clock);
        
        if(liteLoaderOnRenderAdapter != null)
            liteLoaderOnRenderAdapter.trigger(minecraft, partialTicks, inGame, clock);
    }

    @Override
    public String getVersion() {
        return HypixelUtilsCore.VERSION;
    }

    @Override
    public void init(File configFile) {
        try {
            prepareLibs();

            liteloaderCommandRegister = new LiteLoaderCommandRegister();
            HypixelUtilsCore.commandRegister = liteloaderCommandRegister;

            HypixelUtilsCore.eventManager = new EventManager();
            HypixelUtilsCore.initCloudConfig(Paths.get(configFile.getAbsolutePath(), "hypixelutils-configkey.txt"));
            
            HypixelUtilsCore.scheduler = new Scheduler();

            HypixelUtilsCore.init();

            liteloaderOnBungeeServerChangeAdapter = new LiteLoaderOnBungeeServerChangeAdapter(HypixelUtilsCore.eventManager);
            liteloaderOnChatAdapter = new LiteLoaderOnChatAdapter(HypixelUtilsCore.eventManager);
            liteloaderOnTickAdapter = new LiteLoaderOnTickAdapter(HypixelUtilsCore.eventManager);
            liteLoaderOnRenderAdapter = new LiteLoaderOnRenderAdapter(HypixelUtilsCore.eventManager);
            liteLoaderOnConnectAdapter = new LiteLoaderOnConnectAdapter(HypixelUtilsCore.eventManager);
        } catch (Throwable e) {
            e.printStackTrace();
            
            new RuntimeException(e);
        }
    }
    
    private void prepareLibs() {
        Path libsDirPath = Paths.get(LiteLoader.getModsFolder().getAbsolutePath(), "libs");
        File libsDirFile = libsDirPath.toFile();
        if(!libsDirFile.exists()) {
            libsDirFile.mkdirs();
        } else {
            if(!libsDirFile.isDirectory()) {
                throw new RuntimeException(new IOException("Unable to create /mobs/libs directory. Conflicting file name! Refusing to clobber!"));
            }
        }
        
        File libFile;
        
        libFile = Paths.get(libsDirPath.toString(), "hypixelutils-unirest.jar").toFile();
        ensureLib(libFile, "f835f52940a14123da516c21367be7cf", "http://www.cecer1.com/hypixelutils/libs/hypixelutils-unirest.jar");
    }
    
    private void ensureLib(File file, String expectedChecksum, String downloadUrl) {
        boolean fileExisted = file.exists();
        if(!fileExisted) {
            try {
                System.out.println(file.getAbsolutePath() + " does not exist. Downloading from " + downloadUrl);
                URL website = new URL(downloadUrl);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(file);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Downloaded finished. Comparing checksum.");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            if(MD5Checksum.compareMD5Checksum(file.getAbsolutePath(), expectedChecksum)) {
                System.out.println("Successful checksum for " + file.getAbsolutePath() + " - Expected and calculated " + expectedChecksum);
            } else {
                System.out.println("Failed checksum for " + file.getAbsolutePath() + " - Expected "+ expectedChecksum + " but calculated " + expectedChecksum);
                if(fileExisted) {
                    throw new RuntimeException(new IOException("Checksum mismatch on " + file.getAbsolutePath() + " - Corrupt? - Refusing to clobber! Please manually resolve this issue."));
                } else {
                    file.delete();
                    throw new RuntimeException(new IOException("Checksum mismatch on downloaded " + file.getAbsolutePath() + " - Corrupt? - File deleted. Restart the game to retry."));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to calculate checksum for " + file.getAbsolutePath() + " - WARNING: The file may be corrupt but HypixelUtils will assume it is fine!");
        }

        try {
            LiteLoader.getClassLoader().addURL(file.toURI().toURL());
            System.out.println("Added " + file.toURI().toURL() + " to LiteLoader ClassLoader.");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {

    }

    @Override
    public String getName() {
        return HypixelUtilsCore.MODID;
    }

    @Override
    public void onJoinGame(INetHandler netHandler, S01PacketJoinGame joinGamePacket, ServerData serverData, RealmsServer realmsServer) {
        liteLoaderOnConnectAdapter.trigger(serverData);
    }

    @Override
    public List<Class<? extends Packet>> getHandledPackets() {
        List handledPackets = new ArrayList<Class<? extends Packet>>();
        handledPackets.add(S07PacketRespawn.class);
        return handledPackets;
    }

    @Override
    public boolean handlePacket(INetHandler netHandler, Packet packet) {
        if(packet instanceof S07PacketRespawn)
            return onRespawn(netHandler, (S07PacketRespawn)packet);
        return true;
    }

    private boolean onRespawn(INetHandler netHandler, S07PacketRespawn packet) {
        WorldDimension dimension = WorldDimension.fromDimensionId(packet.func_149082_c());
        return liteloaderOnBungeeServerChangeAdapter.onRespawn(dimension);
    }
}
