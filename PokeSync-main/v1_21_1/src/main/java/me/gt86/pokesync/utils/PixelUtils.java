package me.gt86.pokesync.utils;

import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.data.PixelmonPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.util.UUID;

public class PixelUtils {
    public static PCStorage getPCStorage(UUID uuid) {
        return StorageProxy.getPCForPlayerNow(uuid);
    }
    public static PlayerPartyStorage getPartyStorage(UUID uuid) {
        return StorageProxy.getPartyNow(uuid);
    }
    public static ServerPlayer getPlayer(UUID uuid) {
        if (Bukkit.getServer() instanceof CraftServer craftServer) {
            return craftServer.getServer().getPlayerList().getPlayer(uuid);
        }
        return null;
    }
    public static HolderLookup.Provider getRegistry() {
        if (Bukkit.getServer() instanceof CraftServer craftServer) {
            return craftServer.getServer().registryAccess();
        }
        throw new IllegalStateException("Server not loaded");
    }
    public static void sendPacket(UUID uuid, PixelmonPacket object) {
        ServerPlayer player = getPlayer(uuid);
        if (player != null) {
            NetworkHelper.sendPacket(player, object);
        }
    }
}