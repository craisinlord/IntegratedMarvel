const BLOB = "thematic:bat_gel_blob";
const BLEW = "bat_gel_blew";

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:explosivegelgun" });

    component.press(press => {
        press.then("choosing");
    });

    component.phase("choosing", phase => {
        phase.lasts(0.05);
        phase.until({ sneaking: true }, "detonating");
        phase.then("spraying");
    });

    component.phase("detonating", phase => {
        phase.animation("gadget_throw");
        phase.sound("minecraft:block.lever.click", { volume: 1, pitch: 1.6, world: true });
        phase.setFlag(BLEW, { value: false });
        phase.action("integrated_superheroes:gel_detonate", {
            range: tuned("detonate_range"),
            radius: tuned("radius"),
            damage: tuned("damage"),
            push: tuned("push_strength"),
            hardness: tuned("break_hardness"),
            break_radius: tuned("break_radius"),
        });
        phase.lasts(0.1);
        phase.until({ flag: BLEW }, "cooldown");
        phase.then("ready");
    });

    component.phase("spraying", phase => {
        phase.lasts(tuned("spray_lasts"));
        phase.equip({ item: "thematic:explosivegelgun", slot: "mainhand" });
        phase.animation("web_shoot_right");
        phase.sound("minecraft:entity.slime.squish", { volume: 1, pitch: 0.7, world: true });
        phase.sound("minecraft:block.honey_block.place", { volume: 1, pitch: 1.2, world: true });
        phase.projectile({
            body: "bolt",
            from: "main_hand",
            speed: tuned("projectile_speed"),
            maxDistance: tuned("spray_range"),
            damage: 0,
            look: { style: "orb", item: "minecraft:light_blue_dye" },
            trail: { particle: "minecraft:glow", count: 1, spread: 0.05 },
        }, hit => {
            hit.sound("minecraft:entity.slime.squish_small", { at: "target", volume: 1, pitch: 0.8, world: true });
        }, block => {
            block.summon(BLOB, { at: "face", maxAlive: tuned("max_blobs"), lasts: tuned("blob_lasts"), owner: true, hittable: false, solid: false });
            block.sound("minecraft:block.honey_block.slide", { volume: 1, pitch: 1.4, world: true });
            block.particles("minecraft:glow", { count: 8, spread: 0.2, speed: 0.05 });
        });
        phase.then("ready", done => done.stopAnimation("web_shoot_right"));
    });
});
