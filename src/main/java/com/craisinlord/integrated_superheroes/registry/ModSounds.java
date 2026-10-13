package com.craisinlord.integrated_superheroes.registry;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, IntegratedSuperheroes.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_SQUIRREL_IDLE = register("swarm_squirrel_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_SQUIRREL_BEG = register("swarm_squirrel_beg");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_SQUIRREL_NUT = register("swarm_squirrel_nut");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_SQUIRREL_GLIDE_LOOP = register("swarm_squirrel_glide_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_SQUIRREL_HURT = register("swarm_squirrel_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_SQUIRREL_DEATH = register("swarm_squirrel_death");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(IntegratedSuperheroes.id(name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
