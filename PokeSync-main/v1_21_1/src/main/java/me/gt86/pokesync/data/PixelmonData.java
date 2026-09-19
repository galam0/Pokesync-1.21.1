package me.gt86.pokesync.data;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.economy.BankAccount;
import com.pixelmonmod.pixelmon.api.economy.BankAccountProxy;
import com.pixelmonmod.pixelmon.api.enums.ServerCosmetics;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.api.storage.breeding.PlayerDayCare;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.comm.packetHandlers.ServerCosmeticsUpdatePacket;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.UpdateClientPlayerDataPacket;
import com.pixelmonmod.pixelmon.comm.packetHandlers.daycare.SendEntireDayCarePacket;
import com.pixelmonmod.pixelmon.enums.EnumFeatureState;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.enums.EnumMegaItemsUnlocked;
import com.pixelmonmod.pixelmon.enums.EnumTrainerCardColor;
import me.gt86.pokesync.utils.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.user.BukkitUser;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

import static me.gt86.pokesync.utils.PixelUtils.*;

public class PixelmonData {

    private static void setPrivateField(Object instance, String fieldName, Object value) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field: " + fieldName, e);
        }
    }

    public static class PC extends BukkitData implements Data.PC, Adaptable {

        @SerializedName("pc")
        private String nbt;

        private PC(String data) {
            this.nbt = data;
        }

        public PC(PokemonStorage storage) {
            PCStorage pcStorage = (PCStorage) storage;
            LogUtils.debug(String.format("Saving PC for %s | %s pokemon", pcStorage.getPlayerUUID(), pcStorage.countAll()));
            CompoundTag tag = pcStorage.writeToNBT(new CompoundTag(), getRegistry());
            this.nbt = tag.toString();
        }

        @SuppressWarnings("unused")
        private PC() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.PC)) {
                return;
            }
            LogUtils.debug(String.format("Applying PC for %s", user.getUsername()));
            final UUID uuid = user.getUuid();
            PCStorage pcStorage = getPCStorage(uuid);
            try {
                final CompoundTag nbt = TagParser.parseTag(this.nbt);
                pcStorage = pcStorage.readFromNBT(nbt, getRegistry()).join();
                pcStorage.setPlayer(uuid, user.getUsername());
                ServerPlayer player = getPlayer(uuid);
                if (player != null) {
                    // StorageProxy.getStorageManager().initializePCForPlayer(player, pcStorage);
                }
                LogUtils.debug(String.format("Applied PC for %s | %s pokemon)", user.getUsername(), pcStorage.countAll()));
            } catch (CommandSyntaxException e) {
                throw new IllegalStateException("Failed to apply pixelmon pc", e);
            }
        }
    }

    public static class Party extends BukkitData implements Data.Party, Adaptable {

        @SerializedName("party")
        public Map<Integer, String> party;

        public Party(Map<Integer, String> party) {
            this.party = party;
        }

        public Party(PokemonStorage storage) {
            Map<Integer, String> party = new HashMap<>();
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            LogUtils.debug(String.format("Saving party for %s | %s pokemon", partyStorage.getPlayerUUID(), partyStorage.countAll()));
            for (int i = 0; i < PlayerPartyStorage.MAX_PARTY; i++) {
                Pokemon pokemon = partyStorage.get(i);
                if (pokemon != null) {
                    party.put(i, pokemon.writeToNBT(new CompoundTag(), getRegistry()).toString());
                }
            }
            this.party = party;
        }

        @SuppressWarnings("unused")
        private Party() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.PARTY)) {
                return;
            }
            LogUtils.debug(String.format("Applying party for %s", user.getUsername()));
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            storage.tryUpdatePlayerName();
            Map<Integer, String> data = party;
            for (int i = 0; i < PartyStorage.MAX_PARTY; i++) {
                if (data.containsKey(i)) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(data.get(i));
                        Pokemon pokemon = PokemonFactory.create(nbt, getRegistry());
                        storage.set(i, pokemon);
                    } catch (CommandSyntaxException e) {
                        throw new IllegalStateException("Failed to apply pixelmon party", e);
                    }
                } else {
                    storage.set(i, null);
                }
                storage.notifyListeners(new StoragePosition(-1, i), storage.get(i), EnumUpdateType.CLIENT);
            }
            LogUtils.debug(String.format("Applied party for %s | %s pokemon)", user.getUsername(), storage.countAll()));
        }
    }

    public static class Pokedex extends BukkitData implements Data.Pokedex, Adaptable {
        @SerializedName("dex")
        private String dex;
        public Pokedex(String dex) { this.dex = dex; }
        public Pokedex(PokemonStorage storage) { this.dex = "{}"; }
        @SuppressWarnings("unused") private Pokedex() {}
        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {}
    }

    public static class Stats extends BukkitData implements Data.Stats, Adaptable {

        @SerializedName("stats")
        private String stats;

        @SerializedName("starter_picked")
        private boolean starterPicked;

        @SerializedName("battle_enabled")
        private boolean battleEnabled;

        public Stats(String stats, boolean starterPicked, boolean battleEnabled) {
            this.stats = stats;
            this.starterPicked = starterPicked;
            this.battleEnabled = battleEnabled;
        }

        public Stats(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            CompoundTag nbt = new CompoundTag();
            partyStorage.stats.writeToNBT(nbt);
            this.stats = nbt.toString();
            this.starterPicked = partyStorage.starterPicked;
            this.battleEnabled = partyStorage.battleEnabled;
        }

        @SuppressWarnings("unused")
        private Stats() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.STATS)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            try {
                storage.stats.readFromNBT(Objects.requireNonNull(TagParser.parseTag(stats)));
                storage.starterPicked = starterPicked;
                storage.battleEnabled = battleEnabled;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to apply pixelmon stats", e);
            }
        }
    }

    public static class Money extends BukkitData implements Data.Money, Adaptable {

        @SerializedName("money")
        private int money;

        public Money(int money) {
            this.money = money;
        }

        public Money(PokemonStorage storage) {
            UUID uuid = null;
            if (storage instanceof PCStorage pc) {
                uuid = pc.getPlayerUUID();
            } else if (storage instanceof PlayerPartyStorage party) {
                uuid = party.getPlayerUUID();
            } else {
                // 일반적인 경우 리플렉션으로 uuid 필드 직접 추출
                try {
                    java.lang.reflect.Field field = PokemonStorage.class.getDeclaredField("uuid");
                    field.setAccessible(true);
                    uuid = (UUID) field.get(storage);
                } catch (Exception e) {
                    uuid = UUID.randomUUID(); // 예외 방지용 기본값
                }
            }

            BankAccount account = BankAccountProxy.getBankAccountNow(uuid);
            if (account != null) {
                this.money = account.getBalance().intValue();
            } else {
                this.money = 0;
            }
        }

        @SuppressWarnings("unused")
        private Money() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.MONEY)) {
                return;
            }
            final UUID uuid = user.getUuid();
            BankAccount account = BankAccountProxy.getBankAccountNow(uuid);
            if (account != null) {
                account.setBalance(money);
                account.updatePlayer();
            }
        }
    }
    public static class Daycare extends BukkitData implements Data.Daycare, Adaptable {

        @SerializedName("daycare")
        private String daycare;

        public Daycare(String daycare) {
            this.daycare = daycare;
        }

        public Daycare(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            CompoundTag nbt = new CompoundTag();
            partyStorage.getDayCare().writeToNBT(nbt, getRegistry());
            this.daycare = nbt.toString();
        }

        @SuppressWarnings("unused")
        private Daycare() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.DAYCARE)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            try {
                PlayerDayCare dayCare = PlayerDayCare.readFromNBT(storage, TagParser.parseTag(daycare), getRegistry());
                setPrivateField(storage, "dayCare", dayCare);
                sendPacket(uuid, new SendEntireDayCarePacket(dayCare));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to apply pixelmon daycare", e);
            }
        }
    }

    public static class MegaItem extends BukkitData implements Data.MegaItem, Adaptable {

        @SerializedName("mega_items_unlocked")
        private int megaItemsUnlocked;

        @SerializedName("mega_item_string")
        private int megaItemString;

        public MegaItem(int megaItemsUnlocked, int megaItemString) {
            this.megaItemsUnlocked = megaItemsUnlocked;
            this.megaItemString = megaItemString;
        }

        public MegaItem(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            this.megaItemsUnlocked = partyStorage.getMegaItemsUnlocked().ordinal();
            this.megaItemString = partyStorage.getMegaItem().ordinal();
        }

        @SuppressWarnings("unused")
        private MegaItem() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.MEGA_ITEM)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            storage.setMegaItemsUnlocked(EnumMegaItemsUnlocked.values()[megaItemsUnlocked]);
            storage.setMegaItem(EnumMegaItem.values()[megaItemString], false);
            if (PixelmonConfigProxy.getGeneral().isAlwaysHaveMegaRing()) {
                try {
                    if (!storage.getMegaItemsUnlocked().canMega()) {
                        storage.setMegaItem(EnumMegaItem.BraceletORAS, false);
                        storage.unlockMega(true);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to apply pixelmon mega item", e);
                }
            }

            if (PixelmonConfigProxy.getGeneral().isAlwaysHaveDynamaxBand()) {
                try {
                    if (!storage.getMegaItemsUnlocked().canDynamax()) {
                        storage.setMegaItem(EnumMegaItem.DynamaxBand, false);
                        storage.unlockDynamax(true);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to apply pixelmon dynamax item", e);
                }
            }
        }
    }

    public static class Charm extends BukkitData implements Data.Charm, Adaptable {

        @SerializedName("shiny_charm")
        private int shinyCharm;
        @SerializedName("oval_charm")
        private int ovalCharm;

        @SerializedName("exp_charm")
        private int expCharm;

        @SerializedName("catching_charm")
        private int catchingCharm;

        @SerializedName("mark_charm")
        private int markCharm;

        public Charm(int shinyCharm, int ovalCharm, int expCharm, int catchingCharm, int markCharm) {
            this.shinyCharm = shinyCharm;
            this.ovalCharm = ovalCharm;
            this.expCharm = expCharm;
            this.catchingCharm = catchingCharm;
            this.markCharm = markCharm;
        }

        public Charm(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            this.shinyCharm = partyStorage.getShinyCharm().ordinal();
            this.ovalCharm = partyStorage.getOvalCharm().ordinal();
            this.expCharm = partyStorage.getExpCharm().ordinal();
            this.catchingCharm = partyStorage.getCatchingCharm().ordinal();
            this.markCharm = partyStorage.getMarkCharm().ordinal();
        }

        @SuppressWarnings("unused")
        private Charm() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.CHARM)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            storage.setShinyCharm(EnumFeatureState.values()[shinyCharm]);
            storage.setOvalCharm(EnumFeatureState.values()[ovalCharm]);
            storage.setExpCharm(EnumFeatureState.values()[expCharm]);
            storage.setCatchingCharm(EnumFeatureState.values()[catchingCharm]);
            storage.setMarkCharm(EnumFeatureState.values()[markCharm]);
        }
    }

    public static class Gift extends BukkitData implements Data.Gift, Adaptable {

        @SerializedName("gift")
        private String gift;

        public Gift(String gift) {
            this.gift = gift;
        }

        public Gift(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            CompoundTag nbt = new CompoundTag();
            partyStorage.playerData.writeToNBT(nbt);
            this.gift = nbt.toString();
        }

        @SuppressWarnings("unused")
        private Gift() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.GIFTDATA)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            try {
                storage.playerData.readFromNBT(Objects.requireNonNull(TagParser.parseTag(gift)));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to apply pixelmon playerData", e);
            }
        }
    }

    public static class TrainerCard extends BukkitData implements Data.TrainerCard, Adaptable {

        @SerializedName("trainer_card_color")
        private int color;

        public TrainerCard(int color) {
            this.color = color;
        }

        public TrainerCard(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            this.color = partyStorage.trainerCardColor.ordinal();
        }

        @SuppressWarnings("unused")
        private TrainerCard() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.TRAINER_CARD)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            storage.trainerCardColor = EnumTrainerCardColor.values()[color];
            sendPacket(uuid, new UpdateClientPlayerDataPacket(storage.trainerCardColor));
        }
    }

    public static class Cosmetic extends BukkitData implements Data.Cosmetic, Adaptable {

        @SerializedName("cosmetic")
        public byte[] data;

        public Cosmetic(byte[] data) {
            this.data = data;
        }

        public Cosmetic(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            BitSet set = new BitSet();
            Set<ServerCosmetics> serverCosmetics = partyStorage.getServerCosmetics();
            for (ServerCosmetics serverCosmetic : serverCosmetics) {
                set.set(serverCosmetic.ordinal());
            }
            this.data = set.toByteArray();
        }

        @SuppressWarnings("unused")
        private Cosmetic() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.COSMETIC)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            BitSet set = BitSet.valueOf(data);
            Set<ServerCosmetics> serverCosmetics = new HashSet<>();
            for (ServerCosmetics cosmetics : ServerCosmetics.values()) {
                if (set.get(cosmetics.ordinal())) {
                    serverCosmetics.add(cosmetics);
                }
            }
            storage.setServerCosmetics(serverCosmetics);
            sendPacket(uuid, new ServerCosmeticsUpdatePacket(storage.getServerCosmetics()));
        }
    }

    public static class Lure extends BukkitData implements Data.Lure, Adaptable {

        @SerializedName("lure")
        private String data;

        public Lure(String data) {
            this.data = data;
        }

        public Lure(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            ItemStack itemStack = partyStorage.getLureStack();
            if (itemStack != null && !itemStack.isEmpty()) {
                try {
                    ServerPlayer player = getPlayer(partyStorage.getPlayerUUID());
                    if (player != null) {
                        this.data = itemStack.save(player.registryAccess(), new CompoundTag()).toString();
                    } else {
                        this.data = itemStack.toString();
                    }
                } catch (Exception e) {
                    this.data = new CompoundTag().toString();
                }
            } else {
                this.data = "";
            }
        }

        @SuppressWarnings("unused")
        private Lure() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.LURE)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            if (data == null || data.isBlank()) {
                storage.setLureStack(null);
            } else {
                try {
                    ServerPlayer player = getPlayer(uuid);
                    CompoundTag tag = TagParser.parseTag(data);
                    ItemStack itemStack;
                    if (player != null) {
                        itemStack = ItemStack.parse(player.registryAccess(), tag).orElse(ItemStack.EMPTY);
                    } else {
                        itemStack = ItemStack.EMPTY;
                    }
                    storage.setLureStack(itemStack);
                } catch (CommandSyntaxException e) {
                    throw new IllegalStateException("Failed to apply pixelmon lure", e);
                }
            }
        }
    }

    public static class Quest extends BukkitData implements Data.Quest, Adaptable {
        @SerializedName("quest")
        private String data;
        public Quest(String data) { this.data = data; }
        public Quest(PokemonStorage storage) { this.data = "{}"; }
        @SuppressWarnings("unused") private Quest() {}
        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {}
    }

    public static class Curry extends BukkitData implements Data.Curry, Adaptable {

        @SerializedName("curry")
        private int[] data;

        public Curry(int[] data) {
            this.data = data;
        }

        public Curry(PokemonStorage storage) {
            PlayerPartyStorage partyStorage = (PlayerPartyStorage) storage;
            this.data = partyStorage.getCurryData();
        }

        @SuppressWarnings("unused")
        private Curry() {
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            if (!Config.isEnable(PixelmonDataType.CURRY)) {
                return;
            }
            final UUID uuid = user.getUuid();
            PlayerPartyStorage storage = getPartyStorage(uuid);
            int[] currys = data;
            setPrivateField(storage, "curryData", currys);
        }
    }
}