package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.ability.ThematicAbility;
import bond.thematic.api.client.GunKeyActions;
import bond.thematic.api.client.KeyMappingClicks;
import bond.thematic.api.client.KeyMappingDownState;
import bond.thematic.api.compat.CompatConfig;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.registries.entity.AbstractVehicleAbility;
import bond.thematic.api.registries.entity.AbstractVehicleEntity;
import bond.thematic.api.registries.keybind.ThematicKeybind;
import bond.thematic.api.registries.keybind.ThematicKeybinds;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroHudClientConfig;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.HeroSlotsClient;
import com.craisinlord.integrated_superheroes.hero.UnifiedSlots;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Arrays;
import java.util.List;

final class ThematicAbilityKeys {
    static final List<String> KEYS = List.of(
            "key.thematic.ability_0",
            "key.thematic.ability_1",
            "key.thematic.ability_2",
            "key.thematic.ability_3",
            "key.thematic.ability_4",
            "key.thematic.ability_5",
            "key.thematic.ultimate",
            "key.thematic.utility",
            "key.thematic.equip_item",
            "key.thematic.cosmetic");
    static final String PASSIVE_KEY = "key.thematic.passive";
    static final String COSMETIC_KEY = "key.thematic.cosmetic";
    static final int RELOAD = 0;
    static final int COSMETIC = 9;
    static final int UNUSED = -1;
    static final int OVERFLOW = -2;
    private static final String HUD_SECTION = "abilityHud";
    private static final String NATIVE = "native";
    private static final int[] SLOT_OF_KEY = new int[KEYS.size()];
    private static final int[] KEY_AT_SLOT = new int[UnifiedSlots.SLOTS];
    private static final int[] PREVIOUS_KEY_AT_SLOT = new int[UnifiedSlots.SLOTS];
    private static final boolean[] ARMED = new boolean[UnifiedSlots.SLOTS];    private static final KeyMapping[] MAPPINGS = new KeyMapping[KEYS.size()];
    private static Boolean compatHosted;
    private static boolean driving;

    static {
        Arrays.fill(SLOT_OF_KEY, UNUSED);
        Arrays.fill(KEY_AT_SLOT, UNUSED);
        Arrays.fill(PREVIOUS_KEY_AT_SLOT, UNUSED);
    }

    private ThematicAbilityKeys() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ThematicAbilityKeys::onClientTick);
    }

    static int slotOf(int keyIndex) {
        return keyIndex < 0 || keyIndex >= SLOT_OF_KEY.length ? UNUSED : SLOT_OF_KEY[keyIndex];
    }

    static int keyIndex(String keybind) {
        return keybind == null ? UNUSED : KEYS.indexOf(keybind);
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean unified = HeroHudClientConfig.unifiedSuitAbilities();
        syncCompat(unified);
        if (!unified) {
            if (driving) {
                release();
                driving = false;
            }
            return;
        }
        driving = true;
        unbindThematicKeys(minecraft);
        LocalPlayer player = minecraft.player;
        if (player == null) {
            release();
            return;
        }
        layout(player);
        KeyMapping[] keys = HeroSlotsClient.slotKeys();
        boolean[] down = new boolean[KEYS.size()];
        boolean[] owned = new boolean[UnifiedSlots.SLOTS];
        for (int slot = 0; slot < UnifiedSlots.SLOTS; slot++) {
            KeyMapping key = keys[slot];
            int keyIndex = KEY_AT_SLOT[slot];
            if (keyIndex != PREVIOUS_KEY_AT_SLOT[slot]) {
                drain(key);
                ARMED[slot] = false;
                PREVIOUS_KEY_AT_SLOT[slot] = keyIndex;
            }
            boolean raw = rawDown(key);
            if (!raw) {
                ARMED[slot] = true;
            }
            if (keyIndex < 0) {
                continue;
            }
            owned[slot] = true;
            KeyMapping target = mapping(keyIndex);
            if (target == null || !ARMED[slot]) {
                drain(key);
                continue;
            }
            down[keyIndex] |= raw;
            while (key.consumeClick()) {
                ((KeyMappingClicks) target).thematic$addClick();
            }
        }
        UnifiedSlots.setSuitOwned(owned);
        for (int i = 0; i < KEYS.size(); i++) {
            KeyMapping target = mapping(i);
            if (target != null) {
                target.setDown(down[i]);
            }
        }
    }

    private static void layout(Player player) {
        boolean[] used = usedKeys(player);
        int base = UnifiedSlots.firstFree(HeroPowers.data(player));
        Arrays.fill(KEY_AT_SLOT, UNUSED);
        for (int i = 0; i < KEYS.size(); i++) {
            if (!used[i]) {
                SLOT_OF_KEY[i] = UNUSED;
            } else if (base + i < UnifiedSlots.SLOTS) {
                SLOT_OF_KEY[i] = base + i;
                KEY_AT_SLOT[base + i] = i;
            } else {
                SLOT_OF_KEY[i] = OVERFLOW;
            }
        }
    }

    private static boolean[] usedKeys(Player player) {
        boolean[] used = new boolean[KEYS.size()];
        CharacterItem armor = ThematicHelper.getArmor(player);
        if (armor != null) {
            ItemStack stack = ThematicHelper.getArmorStack(player);
            for (ThematicAbility ability : armor.getAbilities(stack)) {
                if (ability == null) {
                    continue;
                }
                String keybind = ability.getKeybind();
                int index = keyIndex(keybind);
                if (index >= 0) {
                    used[index] = true;
                }
                if (!ability.getToggles().isEmpty()) {
                    used[COSMETIC] = true;
                }
            }
            if (player.getVehicle() instanceof AbstractVehicleEntity vehicle) {
                for (AbstractVehicleAbility ability : vehicle.getAbilities()) {
                    int index = ability == null ? UNUSED : keyIndex(ability.getKeybind());
                    if (index >= 0) {
                        used[index] = true;
                    }
                }
            }
        }
        if (GunKeyActions.isHoldingGun()) {
            used[RELOAD] = true;
        }
        if (GunKeyActions.canCycleFireMode()) {
            used[COSMETIC] = true;
        }
        return used;
    }

    private static boolean rawDown(KeyMapping key) {
        return ((KeyMappingDownState) key).thematic$rawDown() && key.getKeyModifier().isActive(key.getKeyConflictContext());
    }

    private static void drain(KeyMapping key) {
        int guard = 0;
        while (key.consumeClick() && guard++ < 1000) {
        }
    }

    private static void release() {
        Arrays.fill(SLOT_OF_KEY, UNUSED);
        Arrays.fill(KEY_AT_SLOT, UNUSED);
        UnifiedSlots.clearSuitOwned();
        for (int i = 0; i < KEYS.size(); i++) {
            KeyMapping target = mapping(i);
            if (target != null) {
                target.setDown(false);
            }
        }
    }

    private static KeyMapping mapping(int index) {
        KeyMapping mapping = MAPPINGS[index];
        if (mapping == null) {
            ThematicKeybind keybind = ThematicKeybinds.getKeybind(KEYS.get(index));
            if (keybind != null) {
                mapping = keybind.getKeyBinding();
                MAPPINGS[index] = mapping;
            }
        }
        return mapping;
    }

    private static void unbindThematicKeys(Minecraft minecraft) {
        boolean changed = false;
        for (int i = 0; i < KEYS.size(); i++) {
            KeyMapping mapping = mapping(i);
            if (mapping != null && (!mapping.isUnbound() || mapping.getKeyModifier() != KeyModifier.NONE)) {
                mapping.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
                changed = true;
            }
        }
        if (changed) {
            KeyMapping.resetMapping();
            if (minecraft.options != null) {
                minecraft.options.save();
            }
        }
    }

    private static void syncCompat(boolean unified) {
        if (compatHosted != null && compatHosted == unified) {
            return;
        }
        compatHosted = unified;
        try {
            CompatConfig.Section section = CompatConfig.CLIENT.section(HUD_SECTION);
            String current = section.getString(ThematicAbilityHud.THEMATIC, NATIVE);
            if (unified && !IntegratedSuperheroes.MODID.equals(current)) {
                section.setString(ThematicAbilityHud.THEMATIC, IntegratedSuperheroes.MODID);
            } else if (!unified && IntegratedSuperheroes.MODID.equals(current)) {
                section.setString(ThematicAbilityHud.THEMATIC, NATIVE);
            }
        } catch (RuntimeException e) {
            IntegratedSuperheroes.LOGGER.warn("Could not update the suit ability HUD host: {}", e.getMessage());
        }
    }
}
