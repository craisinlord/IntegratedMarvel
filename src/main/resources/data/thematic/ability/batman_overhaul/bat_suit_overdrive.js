const SHROUD = "bat_suit_overdrive";

component(component => {
    component.cooldown();
    component.traits({ ultimate: { blocksKeys: false }, wheelIcon: "minecraft:redstone" });
    component.visual("thematic:rising_aura", {
        color: "#7A0606",
        core_color: "#FF2A1A",
        tongues: 20,
        alpha: 0.4,
        embers: 8,
        first_person: 0.25
    });

    component.press(press => {
        press.animation("unleash", { stopAfter: 1.2 });
        press.sound("cloaking_device", { world: true });
        press.sound("minecraft:block.beacon.power_select", { volume: 0.6, pitch: 1.4, world: true });
        press.vfx("thematic:void_shroud", { session: SHROUD, follow: "wearer", color: "#B0C01818" });
        press.particles("minecraft:electric_spark", { count: 30, spread: [0.4, 0.9, 0.4], speed: 0.08, yOffset: 1 });
        press.selfEffect("minecraft:strength", { amplifier: tuned("strength_amplifier"), showParticles: false });
        press.selfEffect("minecraft:speed", { amplifier: tuned("speed_amplifier"), showParticles: false });
        press.selfEffect("minecraft:jump_boost", { amplifier: tuned("jump_amplifier"), showParticles: false });
        press.selfEffect("minecraft:invisibility", { amplifier: tuned("invisibility_amplifier"), showParticles: false });
        press.then("overdrive");
    });

    component.phase("overdrive", overdrive => {
        overdrive.lasts("duration");
        overdrive.every(every => every.vfx("thematic:void_shroud", { session: SHROUD, follow: "wearer", color: "#B0C01818" }));
        overdrive.on("hit_landed", hit => {
            hit.removeEffect("minecraft:invisibility");
            hit.vfx("thematic:void_shroud", { session: SHROUD, stop: true });
            hit.sound("cloaking_device", { pitch: 0.7, world: true });
        });
        overdrive.onExit(exit => {
            exit.vfx("thematic:void_shroud", { session: SHROUD, stop: true });
            exit.removeEffect("minecraft:strength");
            exit.removeEffect("minecraft:speed");
            exit.removeEffect("minecraft:jump_boost");
            exit.removeEffect("minecraft:invisibility");
            exit.animation("thunderclap");
            exit.vfx("thematic:force_shockwave", { radius: tuned("radius") });
            exit.sound("cloaking_device", { pitch: 0.5, world: true });
            exit.sound("minecraft:entity.warden.sonic_boom", { volume: 0.5, pitch: 1.6, world: true });
            exit.shake({ radius: 12, strength: 1.4, duration: 0.5 });
            exit.targets("around", { round: true }, foe => {
                foe.damage({ element: "electric" });
                foe.effect("thematic:stun", { duration: tuned("stun_duration"), amplifier: tuned("stun_amplifier") });
                foe.push("target", { direction: "away_from_owner_horizontal", strength: tuned("push_strength"), lift: tuned("push_lift"), mode: "set" });
                foe.particles("minecraft:electric_spark", { count: 12, spread: 0.4, speed: 0.1 });
            });
        });
    });
});
