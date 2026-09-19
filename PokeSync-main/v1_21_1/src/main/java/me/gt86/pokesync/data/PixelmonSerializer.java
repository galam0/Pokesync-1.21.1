package me.gt86.pokesync.data;

import com.google.gson.Gson;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.Serializer;
import org.jetbrains.annotations.NotNull;

public class PixelmonSerializer {

    // HuskSync 내부 API 버전에 구애받지 않도록 독립적인 Gson 변환기를 직접 사용합니다.
    private static final Gson GSON = new Gson();

    public static abstract class Json<T extends Data> implements Serializer<T> {
        private final Class<T> clazz;

        public Json(@NotNull HuskSync api, Class<T> clazz) {
            this.clazz = clazz;
        }

        @NotNull
        @Override
        public String serialize(@NotNull T data) {
            return GSON.toJson(data);
        }

        @NotNull
        @Override
        public T deserialize(@NotNull String serialized) {
            return GSON.fromJson(serialized, clazz);
        }
    }

    public static class PC extends Json<PixelmonData.PC> {
        public PC(@NotNull HuskSync api) {
            super(api, PixelmonData.PC.class);
        }
    }

    public static class Party extends Json<PixelmonData.Party> {
        public Party(@NotNull HuskSync api) {
            super(api, PixelmonData.Party.class);
        }
    }

    public static class Pokedex extends Json<PixelmonData.Pokedex> {
        public Pokedex(@NotNull HuskSync api) {
            super(api, PixelmonData.Pokedex.class);
        }
    }

    public static class Stats extends Json<PixelmonData.Stats> {
        public Stats(@NotNull HuskSync api) {
            super(api, PixelmonData.Stats.class);
        }
    }

    public static class Money extends Json<PixelmonData.Money> {
        public Money(@NotNull HuskSync api) {
            super(api, PixelmonData.Money.class);
        }
    }

    public static class Daycare extends Json<PixelmonData.Daycare> {
        public Daycare(@NotNull HuskSync api) {
            super(api, PixelmonData.Daycare.class);
        }
    }

    public static class MegaItem extends Json<PixelmonData.MegaItem> {
        public MegaItem(@NotNull HuskSync api) {
            super(api, PixelmonData.MegaItem.class);
        }
    }

    public static class Charm extends Json<PixelmonData.Charm> {
        public Charm(@NotNull HuskSync api) {
            super(api, PixelmonData.Charm.class);
        }
    }

    public static class Gift extends Json<PixelmonData.Gift> {
        public Gift(@NotNull HuskSync api) {
            super(api, PixelmonData.Gift.class);
        }
    }

    public static class TrainerCard extends Json<PixelmonData.TrainerCard> {
        public TrainerCard(@NotNull HuskSync api) {
            super(api, PixelmonData.TrainerCard.class);
        }
    }

    public static class Cosmetic extends Json<PixelmonData.Cosmetic> {
        public Cosmetic(@NotNull HuskSync api) {
            super(api, PixelmonData.Cosmetic.class);
        }
    }

    public static class Lure extends Json<PixelmonData.Lure> {
        public Lure(@NotNull HuskSync api) {
            super(api, PixelmonData.Lure.class);
        }
    }

    public static class Quest extends Json<PixelmonData.Quest> {
        public Quest(@NotNull HuskSync api) {
            super(api, PixelmonData.Quest.class);
        }
    }

    public static class Curry extends Json<PixelmonData.Curry> {
        public Curry(@NotNull HuskSync api) {
            super(api, PixelmonData.Curry.class);
        }
    }
}