package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.action.Actions;
import bond.thematic.api.content.action.ContentAction;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class DraculaActions {
    private static final TagKey<Item> FAMILY = TagKey.create(Registries.ITEM, IntegratedSuperheroes.id("thematic_skin/dracula"));
    private static final ResourceLocation MONSTROUS = ResourceLocation.fromNamespaceAndPath("thematic", "dracula_demonic");
    private static boolean registered;
    private static boolean witcheryFailed;
    private static Object leveling;
    private static Method getLevel;
    private static Object drinker;
    private static Method drink;

    private DraculaActions() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            ResourceLocation id = IntegratedSuperheroes.id("dracula_drink");
            if (!Actions.TYPES.has(id)) {
                Actions.TYPES.register(id, json -> drinkAction());
            }
        } catch (RuntimeException | LinkageError error) {
            IntegratedSuperheroes.LOGGER.warn("Couldn't register the Dracula bite action with Thematic", error);
        }
        NeoForge.EVENT_BUS.addListener(DraculaActions::onEquip);
        NeoForge.EVENT_BUS.addListener(DraculaActions::onTick);
    }

    private static ContentAction drinkAction() {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !(context.hitEntity() instanceof LivingEntity victim)
                    || victim == player || !witchery()) {
                return;
            }
            try {
                drink.invoke(drinker, player, victim);
            } catch (ReflectiveOperationException | RuntimeException error) {
                fail(error);
            }
        };
    }

    @SuppressWarnings("deprecation")
    public static boolean isDracula(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item.builtInRegistryHolder().is(FAMILY) || MONSTROUS.equals(BuiltInRegistries.ITEM.getKey(item));
    }

    public static int requiredLevel() {
        try {
            return ThematicOptions.DRACULA_VAMPIRE_LEVEL.get();
        } catch (IllegalStateException notLoaded) {
            return ThematicOptions.DRACULA_VAMPIRE_LEVEL.getDefault();
        }
    }

    public static boolean allowed(Player player) {
        int needed = requiredLevel();
        if (needed <= 0 || !witchery()) {
            return true;
        }
        try {
            return (int) getLevel.invoke(leveling, player) >= needed;
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail(error);
            return true;
        }
    }

    private static void onEquip(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.CHEST && event.getEntity() instanceof ServerPlayer player && isDracula(event.getTo())) {
            check(player);
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0
                && isDracula(player.getItemBySlot(EquipmentSlot.CHEST))) {
            check(player);
        }
    }

    private static void check(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || allowed(player)) {
            return;
        }
        ItemStack suit = player.getItemBySlot(EquipmentSlot.CHEST).copy();
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        if (!player.getInventory().add(suit)) {
            player.drop(suit, false);
        }
        player.displayClientMessage(Component.translatable("message.integrated_superheroes.dracula_vampire_only", requiredLevel()), true);
    }

    private static boolean witchery() {
        if (witcheryFailed || !ModList.get().isLoaded("witchery")) {
            return false;
        }
        if (getLevel != null && drink != null) {
            return true;
        }
        try {
            Class<?> levelingClass = Class.forName("dev.sterner.witchery.feature.affliction.vampire.VampireLeveling");
            Field instance = levelingClass.getField("INSTANCE");
            leveling = instance.get(null);
            getLevel = levelingClass.getMethod("getLevel", Player.class);
            Class<?> drinkClass = Class.forName("dev.sterner.witchery.content.ability.DrinkBloodAbility");
            drinker = drinkClass.getConstructor().newInstance();
            drink = drinkClass.getMethod("vampireDrinkBloodAbility", ServerPlayer.class, LivingEntity.class);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            fail(error);
            return false;
        }
    }

    private static void fail(Throwable error) {
        if (!witcheryFailed) {
            witcheryFailed = true;
            IntegratedSuperheroes.LOGGER.warn("Dracula's link to Witchery vampires is off; this Witchery version may have changed", error);
        }
    }
}
