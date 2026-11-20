const HOOK = "bat_grapnel_hook";
const LIGHT = { number: { of: input("max_health"), below: tuned("pull_max_health"), who: "target" } };

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:grapplegun", immuneTo: ["minecraft:fall"] });

    component.hold(hold => {
        hold.then("grappling");
    });

    component.phase("grappling", phase => {
        phase.lasts(tuned("lasts"));
        phase.equip({ item: "thematic:grapplegun", slot: "mainhand" });
        phase.animation("web_shoot_right");
        phase.sound("thematic:widow_grapple", { volume: 1, pitch: 1, world: true });
        phase.projectile({
            body: "entity",
            entity: "thematic:bat_grapnel_hook",
            from: "main_hand",
            speed: tuned("projectile_speed"),
            inheritMotion: false,
            keepAs: HOOK,
        }, caught => {
            caught.when(LIGHT, light => {
                light.dropKept(HOOK);
                light.pull({ toward: "owner", strength: tuned("pull_strength"), anchorHeight: 0.5, maxRise: tuned("pull_max_rise") });
                light.effect("thematic:immobile", { duration: tuned("immobile_duration"), amplifier: 0, showParticles: false });
                light.sound("minecraft:entity.fishing_bobber.retrieve", { at: "target", volume: 1.2, pitch: 0.8, world: true });
                light.particles("minecraft:crit", { count: 10, spread: 0.3, speed: 0.15 });
            });
        });
        phase.until({ not: { keeps: HOOK }, inStateFor: 0.9 }, "cooldown");
        phase.on("release", release => {
            release.when({ keeps: HOOK, inStateFor: 0.35 }, flung => {
                flung.dropKept(HOOK);
                flung.push("self", { direction: "look", strength: tuned("fling_strength"), lift: tuned("fling_lift") });
                flung.resetFallDistance();
                flung.animation("web_kickoff");
                flung.sound("thematic:spiderman_swing_whoosh_1", { volume: 1, pitch: 1, world: true });
                flung.particleCircle("circle", { particle: "minecraft:cloud", height: 0.1, radius: 0.8, count: 12 });
            });
            release.then("cooldown");
        });
        phase.onExit(exit => {
            exit.dropKept(HOOK);
        });
    });
});
