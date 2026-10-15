package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GauntletKeyMappings {
    public static final String CATEGORY = "key.categories.integrated_superheroes.gauntlet";
    public static final KeyMapping PREVIOUS_STONE = new KeyMapping(
            "key.integrated_superheroes.gauntlet_previous_stone",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LEFT,
            CATEGORY
    );
    public static final KeyMapping NEXT_STONE = new KeyMapping(
            "key.integrated_superheroes.gauntlet_next_stone",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_RIGHT,
            CATEGORY
    );
    public static final KeyMapping PREVIOUS_ABILITY = new KeyMapping(
            "key.integrated_superheroes.gauntlet_previous_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_UP,
            CATEGORY
    );
    public static final KeyMapping NEXT_ABILITY = new KeyMapping(
            "key.integrated_superheroes.gauntlet_next_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_DOWN,
            CATEGORY
    );
    public static final KeyMapping SPACE_DESTINATION = new KeyMapping(
            "key.integrated_superheroes.gauntlet_space_destination",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
    );

    public static final KeyMapping ABILITY_INFO = new KeyMapping(
            "key.integrated_superheroes.gauntlet_ability_info",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            CATEGORY
    );

    private GauntletKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(PREVIOUS_STONE);
        event.register(NEXT_STONE);
        event.register(PREVIOUS_ABILITY);
        event.register(NEXT_ABILITY);
        event.register(SPACE_DESTINATION);
        event.register(ABILITY_INFO);
    }
}
