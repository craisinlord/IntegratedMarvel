package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public final class SkinData {
    public static final String DEFAULT = "default";
    public static final String OFF = "off";

    public record Choice(String power, String skin, String look, boolean hidden) {
        public static final Choice EMPTY = new Choice("", "", "", false);
        public static final Codec<Choice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("power", "").forGetter(Choice::power),
                Codec.STRING.optionalFieldOf("skin", "").forGetter(Choice::skin),
                Codec.STRING.optionalFieldOf("look", "").forGetter(Choice::look),
                Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Choice::hidden)
        ).apply(instance, Choice::new));
    }

    public record Shown(String skin, String costume) {
        public static final Shown EMPTY = new Shown("", "");
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, IntegratedSuperheroes.MODID);
    public static final Supplier<AttachmentType<Choice>> CHOICE = ATTACHMENTS.register("serum_skin",
            () -> AttachmentType.builder(() -> Choice.EMPTY).serialize(Choice.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Shown>> SHOWN = ATTACHMENTS.register("serum_skin_shown",
            () -> AttachmentType.builder(() -> Shown.EMPTY).build());
    public static final Supplier<AttachmentType<String>> CIVILIAN = ATTACHMENTS.register("civilian_skin",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath().build());
    public static final Supplier<AttachmentType<String>> SUIT_LOOK = ATTACHMENTS.register("suit_skin",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath().build());
    private static ModConfigSpec.BooleanValue enabled;
    private static ModConfigSpec.BooleanValue costumes;

    private SkinData() {
    }

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("serumSkins");
        enabled = builder.comment("Lets serum skins change a hero's name, colours and small traits. When off, every serum uses its base look.")
                .define("enabled", true);
        costumes = builder.comment("Serums draw their hero's costume on the player as soon as they're injected.",
                        "When off, players only see the costume after picking a look with /heroskin. The costume is only a look:",
                        "it doesn't take the chest slot and grants nothing.")
                .define("automaticLooks", true);
        builder.pop();
    }

    public static boolean enabled() {
        try {
            return enabled == null || enabled.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static boolean costumesEnabled() {
        try {
            return costumes != null && costumes.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    public static boolean picked(Player player) {
        String power = power(player);
        if (power.isEmpty() || !player.hasData(CHOICE.get())) {
            return false;
        }
        Choice choice = player.getData(CHOICE.get());
        return choice.power().equals(power) && !choice.hidden();
    }

    public static boolean costumeOff(Player player) {
        return hidden(player) || !costumesEnabled() && !picked(player);
    }

    public static void register(IEventBus modEventBus) {
        SerumSkins.bootstrap();
        SerumLooks.register();
        ATTACHMENTS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(SkinData::onStartTracking);
        NeoForge.EVENT_BUS.addListener(SkinData::onLogin);
        NeoForge.EVENT_BUS.addListener(SkinData::onRespawn);
        NeoForge.EVENT_BUS.addListener(SkinData::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(SkinData::onTick);
        NeoForge.EVENT_BUS.addListener(SkinData::onRegisterCommands);
    }

    public static String power(Player player) {
        String power = HeroPowers.data(player).getPrimaryPower();
        return power == null || "none".equals(power) ? "" : power;
    }

    public static Choice choice(Player player) {
        Choice choice = player.hasData(CHOICE.get()) ? player.getData(CHOICE.get()) : Choice.EMPTY;
        return choice.power().equals(power(player)) ? choice : Choice.EMPTY;
    }

    public static String get(Player player) {
        if (player.level().isClientSide) {
            return player.hasData(SHOWN.get()) ? player.getData(SHOWN.get()).skin() : "";
        }
        Choice choice = choice(player);
        if (choice.hidden() || choice.skin().isEmpty()) {
            return "";
        }
        return SerumSkins.byId(choice.skin()).filter(skin -> skin.powerId().equals(choice.power())).map(SerumSkin::id).orElse("");
    }

    public static String costume(Player player) {
        if (player.level().isClientSide) {
            return player.hasData(SHOWN.get()) ? player.getData(SHOWN.get()).costume() : "";
        }
        String suitLook = suitLook(player);
        if (!suitLook.isEmpty() && SuitSkins.wearsIronMan(player)) {
            return suitLook;
        }
        String civilian = civilian(player);
        if (!civilian.isEmpty()) {
            return civilian;
        }
        String power = power(player);
        if (power.isEmpty() || costumeOff(player) || !SerumCostumes.shows(power, player)) {
            return "";
        }
        Optional<SerumSkin> skin = enabled() ? SerumSkins.byId(get(player)) : Optional.empty();
        String skinId = skin.map(SerumSkin::id).orElse("");
        return SerumLooks.byCostume(power, skinId, choice(player).look()).map(SerumLooks.Look::costume)
                .orElseGet(() -> skin.map(SerumSkin::costume).orElseGet(() -> SerumLooks.preferred(power)));
    }

    public static boolean hidden(Player player) {
        return choice(player).hidden();
    }

    public static String civilian(Player player) {
        String costume = player.hasData(CIVILIAN.get()) ? player.getData(CIVILIAN.get()) : "";
        return CivilianSkins.valid(costume) ? costume : "";
    }

    public static String suitLook(Player player) {
        String costume = player.hasData(SUIT_LOOK.get()) ? player.getData(SUIT_LOOK.get()) : "";
        return SuitSkins.ironManLook(costume) ? costume : "";
    }

    public static void setSuitLook(ServerPlayer player, String costume) {
        if (costume == null || costume.isEmpty()) {
            player.removeData(SUIT_LOOK.get());
        } else {
            player.setData(SUIT_LOOK.get(), costume);
        }
        refresh(player);
    }

    public static void setCivilian(ServerPlayer player, String costume) {
        if (costume == null || costume.isEmpty()) {
            player.removeData(CIVILIAN.get());
        } else {
            player.setData(CIVILIAN.get(), costume);
        }
        refresh(player);
    }

    public static void choose(ServerPlayer player, String skinId) {
        String power = power(player);
        if (power.isEmpty()) {
            return;
        }
        player.removeData(CIVILIAN.get());
        if (OFF.equals(skinId)) {
            player.setData(CHOICE.get(), new Choice(power, "", "", true));
        } else if (skinId == null || skinId.isEmpty() || DEFAULT.equals(skinId)) {
            player.setData(CHOICE.get(), new Choice(power, "", "", false));
        } else {
            player.setData(CHOICE.get(), new Choice(power, skinId, "", false));
        }
        refresh(player);
    }

    public static void chooseLook(ServerPlayer player, SerumLooks.Look look) {
        String power = power(player);
        if (power.isEmpty() || !look.power().equals(power)) {
            return;
        }
        player.removeData(CIVILIAN.get());
        player.setData(CHOICE.get(), new Choice(power, look.skin(), look.costume(), false));
        refresh(player);
    }

    public static String lookId(Player player) {
        Choice choice = choice(player);
        return SerumLooks.byCostume(power(player), choice.skin(), choice.look()).map(SerumLooks.Look::id).orElse("");
    }

    public static void clear(ServerPlayer player) {
        player.removeData(CHOICE.get());
        refresh(player);
    }

    public static void applyClient(Player player, String skinId, String costume) {
        if ((skinId == null || skinId.isEmpty()) && (costume == null || costume.isEmpty())) {
            player.removeData(SHOWN.get());
        } else {
            player.setData(SHOWN.get(), new Shown(skinId == null ? "" : skinId, costume == null ? "" : costume));
        }
    }

    private static Shown resolve(ServerPlayer player) {
        return new Shown(get(player), costume(player));
    }

    private static void refresh(ServerPlayer player) {
        Shown now = resolve(player);
        Shown sent = player.hasData(SHOWN.get()) ? player.getData(SHOWN.get()) : Shown.EMPTY;
        if (!now.equals(sent)) {
            sync(player, now);
        }
    }

    private static void sync(ServerPlayer player) {
        sync(player, resolve(player));
    }

    private static void sync(ServerPlayer player, Shown shown) {
        player.setData(SHOWN.get(), shown);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SerumSkinPayload(player.getId(), shown.skin(), shown.costume()));
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        if (player.hasData(CHOICE.get()) && !player.getData(CHOICE.get()).power().equals(power(player))) {
            player.removeData(CHOICE.get());
        }
        refresh(player);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        SkinCommand.register(event.getDispatcher());
        CivilianSkins.register(event.getDispatcher());
        SuitSkins.register(event.getDispatcher());
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            Shown shown = resolve(target);
            if (!shown.equals(Shown.EMPTY)) {
                PacketDistributor.sendToPlayer(viewer, new SerumSkinPayload(target.getId(), shown.skin(), shown.costume()));
            }
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
