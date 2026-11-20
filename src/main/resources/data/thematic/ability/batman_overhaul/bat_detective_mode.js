const METER = "stalkPercentage";
const METER_MARK = "stalk_meter_body";
const STALK = "stalk";
const STALK_ID = "stalkedEntityId";
const STALK_UUID = "stalkedEntityUuid";
const STALK_UNTIL = "stalkExpiresAt";
const FILLING = "stalkFilling";
const WATCHING = "bat_detective_mode_watching";
const OFF = "bat_detective_mode_off";
const SCAN_AT = "bat_scan_ready_at";
const SCAN_TIME = 1.5;
const BODY = "bat_scan_body_mark";
const SCANNED = "bat_scan_mark";
const AIM = { range: 40, maxAngle: 4, assist: 0.35, lineOfSight: true };

component(component => {
    component.cooldown();
    component.traits({
        wheelIcon: "thematic:batnoculars",
        damageBonus: { times: tuned("stalk_visor_damage_bonus_times"), worn: true, when: { markedBy: { key: STALK } } },
    });
    component.meter({
        key: METER,
        max: tuned("stalk_visor_meter_max"),
        startsAt: tuned("stalk_visor_meter_starts_at"),
        regen: { amount: -1, every: 0.05, chance: 0.2, if: { not: { flag: WATCHING }, until: STALK_UNTIL } },
    });
    component.visual("thematic:vision", {
        shader: "none",
        when: { state: "watching" },
        accent: 0x5555FF,
        label: "Detective Mode",
        icon: "minecraft:lapis_lazuli",
        pingThroughWalls: false,
        scan: true,
        overlay: "thematic:textures/gui/detective_work.png",
        overlayAlpha: 1,
        gauge: {
            meter: METER,
            filling: FILLING,
            until: STALK_UNTIL,
            duration: 30,
            full: "Stalked",
            charging: "Stalking %s%",
            idle: "In plain sight",
        },
    });
    component.visual("thematic:target_portrait", { pin: { uuid: STALK_UUID, id: STALK_ID, until: STALK_UNTIL, duration: 600 } });
    component.visual("thematic:mark_sigil", { key: SCANNED, shape: "ring", color: 0x4FC3FF, alpha: 0.9, size: 0.5, lift: 0.5, seenBy: "owner", throughWalls: true, hud: false });
    component.visual("thematic:target_readout", {
        anchor: "body",
        lines: [
            { key: "integrated_superheroes.bat_scan.name", value: "name" },
            { key: "integrated_superheroes.bat_scan.health", value: "health" },
        ],
        empty: "",
    });

    component.press(press => {
        press.sound("minecraft:block.beacon.activate", { volume: 0.4, pitch: 1.8 });
        press.setFlag(OFF, { value: false });
        press.then("watching");
    });

    component.phase("watching", phase => {
        phase.setFlag(WATCHING);
        phase.on("press", off => {
            off.sound("minecraft:block.beacon.deactivate", { volume: 0.4, pitch: 1.8 });
            off.setFlag(OFF);
        });
        phase.until({ flag: OFF }, "ready");
        phase.every(every => {
            every.selfEffect("minecraft:darkness", { duration: 2, amplifier: tuned("stalk_visor_darkness_amplifier"), showParticles: false, showIcon: false });
            every.targets("aimed", { range: tuned("stalk_visor_target_range"), maxAngle: 1, assist: 0, lineOfSight: false }, aimed => {
                aimed.when({ any: [{ not: { sees: "target" } }, { not: { target: { facing: { toward: "wearer", within: 45 } } } }] }, unseen => {
                    unseen.setFlag(FILLING);
                    unseen.when({ not: { markedBy: { key: METER_MARK } } }, fresh => {
                        fresh.meter(METER, { set: tuned("stalk_visor_meter_set") });
                        fresh.mark(METER_MARK, { for: tuned("stalk_visor_mark_for"), glow: false, max: tuned("stalk_visor_mark_max"), fifo: true });
                    });
                    unseen.meter(METER, { add: tuned("stalk_visor_meter_add") });
                    unseen.when({ meterAtLeast: { meter: METER, amount: 100 } }, full => {
                        full.meter(METER, { set: tuned("stalk_visor_meter_set_2") });
                        full.mark(STALK, { for: tuned("stalk_visor_mark_for_2"), glow: false, max: tuned("stalk_visor_mark_max_2"), fifo: true });
                        full.markTarget(STALK_ID, { uuid: STALK_UUID, keep: true, remember: false });
                        full.markDeadline(STALK_UNTIL, { for: tuned("stalk_visor_mark_deadline_for") });
                    });
                }, seen => {
                    seen.setFlag(FILLING, { value: false });
                });
            }, nobody => {
                nobody.meter(METER, { add: tuned("stalk_visor_meter_add_2") });
                nobody.setFlag(FILLING, { value: false });
            });
        });
        phase.every({ ticks: 2 }, beat => {
            beat.targets("aimed", AIM, aimed => {
                aimed.when({ not: { markedBy: { key: SCANNED } } }, unscanned => {
                    unscanned.when({ not: { markedBy: { key: BODY } } }, fresh => {
                        fresh.mark(BODY, { for: 3, glow: false, max: 1, fifo: true, vfx: "thematic:hunt_mark" });
                        fresh.markDeadline(SCAN_AT, { for: SCAN_TIME });
                    }, held => {
                        held.when({ not: { until: SCAN_AT } }, done => {
                            done.mark(SCANNED, { for: 30, max: 3, fifo: true, glow: true, seenBy: "owner", vfx: "thematic:hunt_mark" });
                            done.action("integrated_superheroes:scan_mark", { seconds: 30, max: 3, bonus: 0.2, boss_bonus: 0.1 });
                            done.vfx("thematic:hunt_bullseye", { from: "eyes", to: "target_body" });
                            done.sound("alexscaves:mine_guardian_scan", { at: "target", volume: 1.2, pitch: 0.8, world: true });
                            done.sound("minecraft:block.note_block.chime", { volume: 0.7, pitch: 1.6, heardBy: "wearer" });
                            done.mark(BODY, { clear: true });
                        });
                    });
                });
            }, nobody => {
                nobody.markDeadline(SCAN_AT, { for: SCAN_TIME });
            });
        });
        phase.onExit(exit => {
            exit.setFlag(WATCHING, { value: false });
            exit.setFlag(FILLING, { value: false });
            exit.setFlag(OFF, { value: false });
            exit.removeEffect("minecraft:darkness");
        });
    });
});
