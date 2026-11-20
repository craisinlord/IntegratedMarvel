const HOLDING = "vader_choke_holding";

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:wither_rose" });

    component.press(press => {
        press.setFlag(HOLDING, { value: false });
        press.targets("locked_or_look", {
            range: tuned("radius"), aimAssist: true, lineOfSight: true, pick: "first", remember: true,
            required: { missCooldown: 0.25 },
        }, victim => {
            victim.action("integrated_superheroes:sith_grip", {
                group: "choke",
                flag: HOLDING,
                max: 1,
                seconds: tuned("duration"),
                player_seconds: tuned("player_hold"),
                lift: tuned("lift"),
                player_lift: tuned("player_lift"),
                damage: tuned("damage"),
                every: tuned("damage_every"),
                break_share: tuned("break_share"),
                boss_share: tuned("boss_share"),
                slowness_seconds: tuned("slowness_duration"),
                slowness_amplifier: tuned("slowness_amplifier"),
            });
            victim.animation("choke_victim", { on: "target", stopAfter: 2 });
            victim.sound("minecraft:entity.player.hurt_drown", { at: "target", volume: 1, pitch: 0.7, world: true });
        });
        press.animation("choke_user", { fadeIn: 0.1 });
        press.then("choking");
    });

    component.phase("choking", phase => {
        phase.lasts("duration");
        phase.until({ not: { flag: HOLDING } }, "cooldown");
        phase.every(0.5, every => {
            every.targets("remembered", victim => {
                victim.sound("minecraft:entity.player.hurt_drown", { at: "target", volume: 0.6, pitch: [0.6, 0.8], world: true });
            });
        });
        phase.onExit(exit => {
            exit.action("integrated_superheroes:sith_release", { group: "choke" });
            exit.setFlag(HOLDING, { value: false });
            exit.stopAnimation("choke_user");
        });
    });
});
