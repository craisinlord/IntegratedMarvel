const METER = "bat_scan_progress";
const BODY = "bat_scan_body_mark";
const SCANNED = "bat_scan_mark";
const AIM = { range: tuned("radius"), maxAngle: 4, assist: 0.35, lineOfSight: true };

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:batnoculars" });
    component.meter({
        key: METER,
        max: tuned("meter_max"),
        startsAt: 0,
        regen: { amount: -4, every: 0.1, if: { not: { holding: "bat_detective_scan" } } },
        hud: { bar: "blue", text: "Scanning" },
    });
    component.visual("thematic:mark_sigil", { key: SCANNED, shape: "ring", color: 0x4FC3FF, alpha: 0.9, size: 0.5, lift: 0.5, seenBy: "owner", throughWalls: true, hud: false });
    component.visual("thematic:target_readout", {
        anchor: "body",
        lines: [
            { key: "integrated_superheroes.bat_scan.name", value: "name" },
            { key: "integrated_superheroes.bat_scan.health", value: "health" },
        ],
        empty: "",
    });

    component.hold(hold => {
        hold.meter(METER, { set: 0 });
        hold.track({ stop: true });
        hold.sound("alexscaves:mine_guardian_scan", { volume: 0.8, pitch: 1.2, world: true });
        hold.then("scanning");
    });

    component.phase("scanning", phase => {
        phase.animation("thor_beam_aim");
        phase.every({ ticks: 2 }, beat => {
            beat.targets("aimed", AIM, aimed => {
                aimed.when({ not: { markedBy: { key: BODY } } }, fresh => {
                    fresh.meter(METER, { set: 0 });
                    fresh.mark(BODY, { for: tuned("lock_for"), glow: false, max: 1, fifo: true, vfx: "thematic:hunt_mark" });
                });
                aimed.meter(METER, { add: tuned("gain") });
                aimed.track({ glow: false });
                aimed.particles("minecraft:electric_spark", { anchor: "body", count: 1, spread: 0.3, speed: 0.02 });
            }, nobody => {
                nobody.meter(METER, { add: tuned("loss", -1) });
            });
        });
        phase.every(0.75, pulse => {
            pulse.targets("marked", { key: BODY, hittable: false, within: tuned("radius") }, aimed => {
                aimed.vfx("thematic:hunt_mark");
                aimed.sound("alexscaves:mine_guardian_scan", { at: "target", volume: 0.6, pitch: 1.4, world: true });
            });
        });
        phase.until({ meterAtLeast: { meter: METER, amount: 100 } }, "cooldown", full => {
            full.meter(METER, { set: 0 });
            full.targets("marked", { key: BODY, hittable: false }, scanned => {
                scanned.mark(SCANNED, { for: tuned("duration"), max: 3, fifo: true, glow: true, seenBy: "owner", vfx: "thematic:hunt_mark" });
                scanned.action("integrated_superheroes:scan_mark", { seconds: tuned("duration"), max: tuned("max_scanned"), bonus: tuned("damage_bonus"), boss_bonus: tuned("boss_damage_bonus") });
                scanned.vfx("thematic:hunt_bullseye", { from: "eyes", to: "target_body" });
                scanned.sound("alexscaves:mine_guardian_scan", { at: "target", volume: 1.2, pitch: 0.8, world: true });
                scanned.sound("minecraft:block.note_block.chime", { volume: 0.7, pitch: 1.6, heardBy: "wearer" });
                scanned.mark(BODY, { clear: true });
            });
        });
        phase.on("release", release => {
            release.targets("marked", { key: BODY, hittable: false }, aimed => aimed.mark(BODY, { clear: true }));
            release.then("ready");
        });
        phase.onExit(exit => {
            exit.stopAnimation("thor_beam_aim");
            exit.track({ stop: true });
        });
    });
});
