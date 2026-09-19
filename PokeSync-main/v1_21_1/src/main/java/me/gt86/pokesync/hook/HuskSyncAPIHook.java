package me.gt86.pokesync.hook;

import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import me.gt86.pokesync.PokeSync;
import me.gt86.pokesync.data.Config;
import me.gt86.pokesync.data.PixelmonDataType;
import me.gt86.pokesync.utils.LogUtils;
import net.william278.husksync.HuskSync;
import net.william278.husksync.api.HuskSyncAPI;
import net.william278.husksync.event.BukkitDataSaveEvent;
import net.william278.husksync.event.BukkitPreSyncEvent;
import net.william278.husksync.event.BukkitSyncCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HuskSyncAPIHook implements Listener {

    private final HuskSyncAPI huskSyncAPI;
    private final boolean usePokeSyncMixins;

    public HuskSyncAPIHook() {
        this.huskSyncAPI = HuskSyncAPI.getInstance();
        this.usePokeSyncMixins = PokeSync.getInstance().isUsePokeSyncMixins();
        register();
    }

    private void register() {
        HuskSync huskSync = huskSyncAPI.getPlugin();
        for (PixelmonDataType type : PixelmonDataType.values()) {
            if (Config.isEnable(type)) {
                huskSyncAPI.registerDataSerializer(type.getIdentifier(), type.createSerializer(huskSync));
            }
        }
        Bukkit.getPluginManager().registerEvents(this, PokeSync.getInstance());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBukkitDataSave(BukkitDataSaveEvent event) {
        event.editData(unpacked -> {
            UUID uuid = event.getUser().getUuid();
            PCStorage pc = StorageProxy.getPCForPlayerNow(uuid);
            PlayerPartyStorage party = StorageProxy.getPartyNow(uuid);

            // 객체 충돌 방지를 위해 기존 pokesync 데이터 찌꺼기 청소
            unpacked.getData().keySet().removeIf(id -> id.toString().startsWith("pokesync:"));

            for (PixelmonDataType type : PixelmonDataType.values()) {
                if (Config.isEnable(type)) {
                    // 💡 타입별로 올바른 데이터 객체를 매칭하도록 수정합니다.
                    if (type == PixelmonDataType.PC) {
                        if (pc != null) {
                            unpacked.setData(type.getIdentifier(), type.createData(pc));
                        }
                    } else if (type == PixelmonDataType.PARTY) {
                        if (party != null) {
                            unpacked.setData(type.getIdentifier(), type.createData(party));
                        }
                    } else {
                        // 그 외 기타 데이터 타입들 (돈, 도감 등 필요에 따라 처리)
                        unpacked.setData(type.getIdentifier(), type.createData(party));
                    }

                    LogUtils.debug(String.format("Data saved for %s for %s", event.getUser().getUsername(), type.getIdentifier()));
                }
            }
            LogUtils.debug(String.format("PokeSync saved for data [%s]", unpacked.getId()));
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBukkitSyncComplete(BukkitSyncCompleteEvent event) {
        UUID uuid = event.getUser().getUuid();
        PlayerPartyStorage party = StorageProxy.getPartyNow(uuid);
        PCStorage pc = StorageProxy.getPCForPlayerNow(uuid);

        if (party != null) {
            StorageProxy.getSaveScheduler().save(party, me.gt86.pokesync.utils.PixelUtils.getRegistry());
            LogUtils.debug(String.format("Load Party saved for %s | %d pokemon", event.getUser().getUsername(), party.countAll()));
        }
        if (pc != null) {
            StorageProxy.getSaveScheduler().save(pc, me.gt86.pokesync.utils.PixelUtils.getRegistry());
            LogUtils.debug(String.format("Load PC saved for %s | %d pokemon", event.getUser().getUsername(), pc.countAll()));
        }
        if (usePokeSyncMixins) {
            PokeSyncMixinsHook.callCompleteEvent(uuid);
        }
    }
}