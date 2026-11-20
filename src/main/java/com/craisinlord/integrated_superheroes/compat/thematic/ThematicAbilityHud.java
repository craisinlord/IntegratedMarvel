package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.ability.ThematicAbility;
import bond.thematic.api.client.CooldownFeedback;
import bond.thematic.api.client.GunKeyActions;
import bond.thematic.api.client.hud.AbilityHudEntry;
import bond.thematic.api.client.hud.AbilityHudSource;
import bond.thematic.api.client.hud.AbilityHudStyles;
import bond.thematic.api.content.legacy.api.AbilityKinds;
import bond.thematic.api.content.legacy.mixin.LoadoutQueries;
import bond.thematic.api.content.loadout.Loadouts;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.registries.data.EntityComponents;
import bond.thematic.api.registries.data.inventory.NBTInventory;
import bond.thematic.api.registries.entity.AbstractVehicleAbility;
import bond.thematic.api.registries.entity.AbstractVehicleEntity;
import bond.thematic.api.registries.item.ThematicArrowItem;
import bond.thematic.api.registries.keybind.ThematicKeybinds;
import bond.thematic.api.util.AbilityControl;
import bond.thematic.api.util.AbilityUtils;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.AbilityRow;
import com.craisinlord.integrated_superheroes.hero.HeroHudClientConfig;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.HeroSlots;
import com.craisinlord.integrated_superheroes.hero.HeroSlotsClient;
import com.craisinlord.integrated_superheroes.hero.UnifiedSlots;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class ThematicAbilityHud implements UnifiedSlots.SuitRows, AbilityHudSource {
    static final String THEMATIC = "thematic";
    private static final ThematicAbilityHud INSTANCE = new ThematicAbilityHud();

    private record Gadget(ItemStack icon, Component label) {
    }

    private ThematicAbilityHud() {
    }

    static void init() {
        UnifiedSlots.setSuitRows(INSTANCE);
        AbilityHudStyles.register(INSTANCE);
        UnifiedSlots.setHostedElsewhere(() -> !AbilityHudStyles.drawsOwn(IntegratedSuperheroes.MODID));
    }

    @Override
    public String modId() {
        return IntegratedSuperheroes.MODID;
    }

    @Override
    public boolean showsForeignAbilities() {
        return HeroHudClientConfig.unifiedSuitAbilities();
    }

    @Override
    public List<AbilityHudEntry> entries(Player player) {
        List<AbilityHudEntry> entries = new ArrayList<>();
        if (player == null) {
            return entries;
        }
        PlayerData data = HeroPowers.data(player);
        KeyMapping[] keys = HeroSlotsClient.slotKeys();
        for (int slot = 0; slot < HeroSlots.SLOTS && slot < data.getActivePowers().size(); slot++) {
            Power power = data.getActivePower(slot);
            if (power == null || "none".equals(power.getName())) {
                continue;
            }
            entries.add(new AbilityHudEntry(Component.literal(power.getDisplayName()), keys[slot].getTranslatedKeyMessage(),
                    (int) Math.round(power.getCooldown() * 20.0D), 0, power.isActive(), ItemStack.EMPTY));
        }
        return entries;
    }

    @Override
    public List<AbilityRow> rows(Player player, KeyMapping[] keys) {
        List<AbilityRow> rows = new ArrayList<>();
        CharacterItem armor = ThematicHelper.getArmor(player);
        if (armor != null) {
            if (player.getVehicle() instanceof AbstractVehicleEntity vehicle) {
                vehicleRows(rows, player, vehicle, keys);
            } else {
                suitRows(rows, player, armor, keys);
            }
        }
        if (GunKeyActions.isHoldingGun() && !hasRowOn(rows, ThematicAbilityKeys.RELOAD)) {
            rows.add(simple(Component.literal("Reload"), ThematicAbilityKeys.RELOAD, keys));
        }
        if (GunKeyActions.canCycleFireMode() && !hasRowOn(rows, ThematicAbilityKeys.COSMETIC)) {
            rows.add(simple(Component.literal("Fire Mode"), ThematicAbilityKeys.COSMETIC, keys));
        }
        return rows;
    }

    private static boolean hasRowOn(List<AbilityRow> rows, int keyIndex) {
        int slot = ThematicAbilityKeys.slotOf(keyIndex);
        return slot >= 0 && rows.stream().anyMatch(row -> row.slot() == slot);
    }

    private static AbilityRow simple(Component name, int keyIndex, KeyMapping[] keys) {
        return AbilityRow.simple(name, rowSlot(keyIndex), keyText(keyIndex, keys), true);
    }

    private static int rowSlot(int keyIndex) {
        int slot = ThematicAbilityKeys.slotOf(keyIndex);
        return slot >= 0 ? slot : -1;
    }

    private static Component keyText(int keyIndex, KeyMapping[] keys) {
        int slot = ThematicAbilityKeys.slotOf(keyIndex);
        return slot >= 0 ? keys[slot].getTranslatedKeyMessage() : Component.literal("?");
    }

    private static void suitRows(List<AbilityRow> rows, Player player, CharacterItem armor, KeyMapping[] keys) {
        List<ThematicAbility> abilities = new ArrayList<>(armor.getAbilities(ThematicHelper.getArmorStack(player)));
        List<ThematicAbility> wheel = new ArrayList<>();
        List<ThematicAbility> consolidated = new ArrayList<>();
        List<ThematicAbility> keyed = new ArrayList<>();
        List<ThematicAbility> passive = new ArrayList<>();
        for (ThematicAbility ability : abilities) {
            if (ability == null) {
                continue;
            }
            String keybind = ability.getKeybind();
            boolean passiveKey = ThematicAbilityKeys.COSMETIC_KEY.equals(keybind) || ThematicAbilityKeys.PASSIVE_KEY.equals(keybind);
            boolean visible = ability.isVisible() && keybind != null
                    && (!ThematicAbilityKeys.PASSIVE_KEY.equalsIgnoreCase(keybind) || !ability.getToggles().isEmpty());
            if (passiveKey || keybind == null || !ability.getToggles().isEmpty()) {
                wheel.add(ability);
                if (visible && (passiveKey || keybind == null)) {
                    consolidated.add(ability);
                }
            }
            (passiveKey ? passive : keyed).add(ability);
        }
        keyed.addAll(passive);
        Gadget gadget = HeroHudClientConfig.suitGadgetsInHud() ? gadget(player, armor) : null;
        for (ThematicAbility ability : keyed) {
            if (!ability.isVisible() || ability.getKeybind() == null) {
                continue;
            }
            boolean isConsolidated = consolidated.contains(ability);
            if (!isConsolidated && ThematicAbilityKeys.PASSIVE_KEY.equalsIgnoreCase(ability.getKeybind())) {
                continue;
            }
            if (isConsolidated && consolidated.size() > 1) {
                if (ability == consolidated.get(0)) {
                    rows.add(simple(Component.literal(togglesLabel(consolidated)), ThematicAbilityKeys.COSMETIC, keys));
                }
                continue;
            }
            rows.add(abilityRow(player, ability, isConsolidated, wheel.contains(ability), gadget, keys));
        }
    }

    private static String togglesLabel(List<ThematicAbility> consolidated) {
        boolean vehicles = false;
        boolean others = false;
        for (ThematicAbility ability : consolidated) {
            if (AbilityKinds.is(ability, "vehicle")) {
                vehicles = true;
            } else {
                others = true;
            }
        }
        return vehicles && !others ? "Vehicles" : (!vehicles ? "Toggle Passives" : "Toggles");
    }

    private static AbilityRow abilityRow(Player player, ThematicAbility ability, boolean isConsolidated, boolean isWheel, Gadget gadget, KeyMapping[] keys) {
        ThematicAbility shown = ability;
        if (!isConsolidated) {
            ThematicAbility standIn = ability.shownAs(player);
            if (standIn != null) {
                shown = standIn;
            }
        }
        int keyIndex = isConsolidated || ThematicAbilityKeys.COSMETIC_KEY.equals(shown.getKeybind())
                ? ThematicAbilityKeys.COSMETIC
                : ThematicAbilityKeys.keyIndex(ability.getKeybind());
        boolean held = AbilityUtils.isHeldDown(player, shown.getId());
        boolean blocked = shown.isBlocked(player);
        AbilityRow.Lock lock = AbilityRow.Lock.NONE;
        if (!held && AbilityControl.blockedByExclusive(player, shown)) {
            lock = AbilityRow.Lock.EXCLUSIVE;
        } else if (!held && blocked) {
            lock = AbilityRow.Lock.BLOCKED;
        }
        boolean active = held || AbilityUtils.isActive(player, shown.getId());
        Component name = shown.display(player, blocked);
        if (empty(name)) {
            name = isWheel && !shown.getToggles().isEmpty()
                    ? Component.translatable(shown.getToggles().get(0).name())
                    : Component.literal("Unknown Ability").withStyle(ChatFormatting.RED);
        }
        MutableComponent suffix = Component.empty();
        Component thematicSuffix = shown.suffix(player);
        if (!empty(thematicSuffix)) {
            suffix.append(thematicSuffix);
        }
        ThematicAbility shownIcon = shown;
        ItemStack icon = shown.options().icon().orElseGet(() -> {
            ItemStack wheel = shownIcon.getWheelIcon();
            return wheel.is(net.minecraft.world.item.Items.FEATHER) ? ItemStack.EMPTY : wheel;
        });
        if (gadget != null && (Loadouts.loadoutOf(ability) != null || Loadouts.loadoutOf(shown) != null)) {
            icon = gadget.icon();
            suffix.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(gadget.label().copy().withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        }
        if (shown.showCosmeticKeybind() && isWheel && keyIndex != ThematicAbilityKeys.COSMETIC
                && ThematicAbilityKeys.slotOf(ThematicAbilityKeys.COSMETIC) >= 0) {
            suffix.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(keyText(ThematicAbilityKeys.COSMETIC, keys))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        }
        double cooldown = active ? 0.0D : shown.getCooldown(player) / 20.0D;
        return new AbilityRow(name, rowSlot(keyIndex), keyText(keyIndex, keys), cooldown, active, lock, suffix,
                icon, true, CooldownFeedback.emphasised(player, shown));
    }

    private static void vehicleRows(List<AbilityRow> rows, Player player, AbstractVehicleEntity vehicle, KeyMapping[] keys) {
        for (AbstractVehicleAbility ability : vehicle.getAbilities()) {
            if (ability == null || !ability.isVisible() || ability.getKeybind() == null
                    || ability.getKeybind().equalsIgnoreCase(ThematicAbilityKeys.PASSIVE_KEY)
                    || ability.getKeybind().equalsIgnoreCase(ThematicAbilityKeys.COSMETIC_KEY)) {
                continue;
            }
            int keyIndex = ThematicAbilityKeys.keyIndex(ability.getKeybind());
            boolean held = AbstractVehicleAbility.isHeldDown(player, ability.getId());
            boolean blocked = ability.isBlocked(player);
            boolean active = held || AbstractVehicleAbility.isActive(vehicle, ability.getId());
            Component name = ability.display(vehicle, blocked);
            if (empty(name)) {
                name = Component.literal("Unknown Ability").withStyle(ChatFormatting.RED);
            }
            Component suffix = ability.suffix(player);
            double cooldown = active ? 0.0D : ability.getCooldown(vehicle) / 20.0D;
            rows.add(new AbilityRow(name, rowSlot(keyIndex), keyText(keyIndex, keys), cooldown, active,
                    !held && blocked ? AbilityRow.Lock.BLOCKED : AbilityRow.Lock.NONE,
                    empty(suffix) ? Component.empty() : suffix, ItemStack.EMPTY, true, false));
        }
        if (ThematicKeybinds.getRaiseSpeed() != null) {
            rows.add(AbilityRow.simple(Component.literal("Zoom In"), UnifiedSlots.SLOTS,
                    ThematicKeybinds.getRaiseSpeed().getKeyBinding().getTranslatedKeyMessage(), true));
        }
        if (ThematicKeybinds.getLowerSpeed() != null) {
            rows.add(AbilityRow.simple(Component.literal("Zoom Out"), UnifiedSlots.SLOTS + 1,
                    ThematicKeybinds.getLowerSpeed().getKeyBinding().getTranslatedKeyMessage(), true));
        }
    }

    private static Gadget gadget(Player player, CharacterItem armor) {
        LoadoutQueries.HudLoadout loadout = LoadoutQueries.hudLoadout(armor, player);
        if (loadout == null || !(loadout.inventory() instanceof NBTInventory inventory)) {
            return null;
        }
        int selected = inventory.selectedSlot;
        List<Integer> shown = loadout.shownSlots();
        ItemStack stack = selected != -1 && shown != null && shown.contains(selected)
                ? inventory.getItem(selected)
                : EntityComponents.INVENTORIES.get(player).getDefaultItem(loadout.inventoryName());
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        MutableComponent label = stack.getHoverName().copy();
        if (stack.getCount() > 1) {
            label.append(" x" + stack.getCount());
        }
        return new Gadget(ThematicArrowItem.displayOf(stack), label);
    }

    private static boolean empty(Component component) {
        return component == null || component.getString().isEmpty();
    }
}
