const FEAR = "bat_fear";
const PREY = "bat_fear_mark";
const BOSS = { target: { entity: "#c:bosses" } };
const UNSEEN = {
    attackerKind: "self",
    any: [
        { target: { not: { facing: { toward: "wearer", within: 70 } } } },
        { hasEffect: "minecraft:invisibility" },
        { sneaking: true },
    ],
};

function takedown(actions) {
    actions.targets("marked", { key: PREY, pick: "nearest", within: tuned("radius", 1.5) }, prey => {
        prey.vfx("thematic:bat_stream", { from: "chest", to: "target_body" });
        prey.teleport("behind_target", { safe: true, distance: 1.2 });
        prey.lookAt();
        prey.damage({ source: "player_attack" });
        prey.vfx("thematic:bat_scatter", { anchor: "body" });
        prey.sound("minecraft:item.mace.smash_ground", { at: "target", volume: 1.2, pitch: 1, world: true });
        prey.particles("minecraft:crit", { anchor: "body", count: 16, spread: 0.3, speed: 0.3 });
        prey.hitstop({ duration: 0.08 });
        prey.shake({ radius: 12, strength: 1.2, duration: 0.35 });
        prey.knockback({ strength: tuned("knockback_strength"), direction: "look" });
        prey.when({ not: BOSS }, small => {
            small.effect("thematic:stun", { duration: tuned("stun_duration"), amplifier: 0 });
            small.effect("minecraft:darkness", { duration: tuned("stun_duration"), amplifier: 0, showParticles: false });
            small.effect("minecraft:weakness", { duration: tuned("weakness_duration"), amplifier: tuned("weakness_amplifier") });
        });
        prey.mark(PREY, { clear: true });
    });
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:fear_shard", ultimate: true, stunImmune: true, immuneTo: ["minecraft:fall"] });
    component.requires({ meterAtLeast: { meter: FEAR, amount: 100 } }, { message: "integrated_superheroes.bat_fear.not_ready" });
    component.meter({
        key: FEAR,
        max: tuned("meter_max"),
        startsAt: 0,
        persistent: true,
        hud: { bar: "dark_red", text: "Fear %1$s / %2$s" },
    });

    component.on("hit_landed", UNSEEN, hit => {
        hit.meter(FEAR, { add: tuned("fear_per_hit") });
    });
    component.on("kill", { attackerKind: "self" }, kill => {
        kill.meter(FEAR, { add: tuned("fear_per_kill") });
    });

    component.press(press => {
        press.targets("around", { within: tuned("radius"), round: true, lineOfSight: true, required: true }, prey => {
            prey.mark(PREY, { for: tuned("duration"), glow: false });
        });
        press.meter(FEAR, { set: 0 });
        press.sound("thematic:cloaking_device", { volume: 1, pitch: 0.6, world: true });
        press.sound("minecraft:entity.bat.takeoff", { volume: 1.2, pitch: 0.7, world: true });
        press.vfx("thematic:bat_swirl");
        press.grade({ desaturate: 0.85, keepHue: "red", contrast: 0.25, duration: tuned("duration", 1.2), fadeIn: 0.25, fadeOut: 0.6 });
        press.particleCircle("thick_smoke", { particle: "thematic:thick_smoke", height: 0, radius: 3, count: 40, yOffset: 1 });
        press.then("takedown");
        press.wait(0.25);
        press.animation("flying_kick");
        takedown(press);
        press.wait(0.45);
        press.animation("perfect_counter");
        takedown(press);
        press.wait(0.45);
        press.animation("running_jumpkick");
        takedown(press);
    });

    component.phase("takedown", phase => {
        phase.lasts("duration");
        phase.use("thematic:slow_motion", { radius: tuned("radius", 1.5), self: false, victims: { lockMoves: true, motion: tuned("slow_motion") } });
        phase.every(0.1, every => {
            every.particles("minecraft:large_smoke", { anchor: "body", count: 1, spread: 0.3, speed: 0.01 });
        });
        phase.onExit(exit => {
            exit.targets("marked", { key: PREY, hittable: false }, left => left.mark(PREY, { clear: true }));
            exit.vfx("thematic:bat_scatter");
        });
    });
});
