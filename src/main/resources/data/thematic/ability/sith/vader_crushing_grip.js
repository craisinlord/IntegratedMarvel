const CRUSHING = "vader_crush_holding";

component(component => {
    component.cooldown();
    component.traits({ ultimate: { blocksKeys: false }, wheelIcon: "minecraft:netherite_scrap", stunImmune: true });

    component.press(press => {
        press.setFlag(CRUSHING, { value: false });
        press.targets("around", { radius: tuned("radius"), round: true, arc: tuned("grip_arc"), lineOfSight: true, sort: "aim", required: { missCooldown: 0.25 } }, victim => {
            victim.action("integrated_superheroes:sith_grip", {
                group: "crush",
                flag: CRUSHING,
                max: tuned("max_targets"),
                seconds: tuned("duration"),
                player_seconds: tuned("player_hold"),
                lift: tuned("lift"),
                player_lift: tuned("player_lift"),
                damage: tuned("damage"),
                every: tuned("damage_every"),
                boss_share: tuned("boss_share"),
                slowness_seconds: tuned("slowness_duration"),
                slowness_amplifier: tuned("slowness_amplifier"),
            });
            victim.animation("choke_victim", { on: "target", stopAfter: 1.5 });
        });
        press.animation("sonic_lift", { fadeIn: 0.1 });
        press.sound("minecraft:entity.warden.heartbeat", { volume: 1.5, pitch: 0.6, world: true });
        press.shake({ radius: 14, strength: 1.2, duration: 0.5 });
        press.vfx("thematic:slam_dust_large");
        press.then("crushing");
    });

    component.phase("crushing", phase => {
        phase.lasts("duration");
        phase.until({ not: { flag: CRUSHING } }, "cooldown");
        phase.every(1, every => {
            every.sound("minecraft:entity.warden.heartbeat", { volume: 1.2, pitch: 0.6, world: true });
            every.shake({ radius: 10, strength: 0.6, duration: 0.3 });
        });
        phase.onExit(exit => {
            exit.action("integrated_superheroes:sith_release", { group: "crush" });
            exit.setFlag(CRUSHING, { value: false });
            exit.stopAnimation("sonic_lift");
            exit.vfx("thematic:slam_dust_large");
            exit.sound("minecraft:entity.generic.explode", { volume: 0.6, pitch: 0.6, world: true });
        });
    });
});
