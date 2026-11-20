const SETTLE = "bat_snow_cloak_settle";
const HIDDEN = "bat_snow_cloak_hidden";
const GROUND = ["minecraft:snow_block", "minecraft:snow", "minecraft:powder_snow", "#minecraft:ice"];
const LAYERS = ["minecraft:snow", "minecraft:powder_snow"];

function crouchedOnSnow() {
    return {
        sneaking: true,
        still: true,
        any: [
            { insideBlock: { blocks: GROUND, at: "below" } },
            { insideBlock: { blocks: LAYERS, at: "feet" } }
        ]
    };
}

function settled() {
    const guard = crouchedOnSnow();
    guard.not = { until: SETTLE };
    return guard;
}

function reveal(actions) {
    actions.markDeadline(SETTLE, { for: tuned("settle_time") });
    actions.when({ flag: HIDDEN }, shown => {
        shown.setFlag(HIDDEN, { value: false });
        shown.removeEffect("minecraft:invisibility");
        shown.particles("minecraft:snowflake", { count: 12, spread: [0.4, 0.8, 0.4], speed: 0.03, yOffset: 1 });
    });
}

component(component => {
    component.traits({ wheelIcon: "minecraft:snowball" });

    component.on("tick", { every: 0.25 }, tick => {
        tick.when(settled(), hidden => {
            hidden.when({ not: { flag: HIDDEN } }, fresh => {
                fresh.setFlag(HIDDEN, { value: true });
                fresh.animation("arm_wrap");
                fresh.vfx("thematic:powergirl_frost_mist", { radius: 1.2, duration: 30 });
                fresh.particles("minecraft:snowflake", { count: 30, spread: [0.6, 0.9, 0.6], speed: 0.02, yOffset: 0.8 });
                fresh.sound("frost_breath", { volume: 0.25, pitch: 1.2, world: true });
            });
            hidden.selfEffect("minecraft:invisibility", { duration: tuned("invisibility_duration"), amplifier: tuned("invisibility_amplifier"), showParticles: false, showIcon: false });
            hidden.targets("around", { radius: tuned("radius"), hittable: false }, mob => {
                mob.when({ huntedBy: "target" }, hunter => hunter.calm());
            });
        }, exposed => {
            exposed.when({ not: crouchedOnSnow() }, moved => reveal(moved));
        });
    });

    component.on("swing", swing => reveal(swing));
    component.on("hit_landed", hit => reveal(hit));
});
