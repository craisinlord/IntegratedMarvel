const GUN = "thematic:harpoon_gun";
const ANCHOR = "bat_harpoon_anchor";

const BIG = {
    any: [
        { number: { of: expr("target_height - @big_height"), at_least: 0 } },
        { target: { entity: "#c:bosses" } },
        { target: { mob: false } }
    ]
};

function reelSelf(actions) {
    actions.vfx("thematic:harpoon_beam", { from: "right_hand", to: "spot" });
    actions.sound("alexscaves:candy_cane_hook_reel", { world: true });
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: GUN, immuneTo: ["minecraft:fall"] });
    component.requires({ inWater: true });

    component.press(press => {
        press.animation("web_shoot_right", { priority: 1, fadeIn: 0.05 });
        press.sound("alexscaves:candy_cane_hook_launch", { world: true });
        press.then("firing");
    });

    component.phase("firing", firing => {
        firing.use("thematic:equip", { item: GUN, slot: "mainhand", displaced: "stash", keep: true });
        firing.lasts(tuned("fire_for"));
        firing.particles("minecraft:bubble", { count: 16, spread: 0.1, speed: 0.2, from: "right_hand" });
        firing.targets("aimed", { range: tuned("radius"), hittable: false, lineOfSight: true }, caught => {
            caught.vfx("thematic:harpoon_beam", { to: "wearer" });
            caught.when(BIG, big => {
                big.markSpot(ANCHOR, { anchor: "body" });
                big.damage({ source: "player_attack" });
            }, small => {
                small.damage({ source: "player_attack" });
                small.pull({
                    toward: "owner",
                    strength: tuned("pull_strength"),
                    for: tuned("pull_for"),
                    stopWithin: tuned("pull_stop_within"),
                    point: "center",
                    beam: "harpoon"
                });
                small.sound("alexscaves:candy_cane_hook_reel", { at: "target", world: true });
            });
        }, nobody => {
            nobody.markSpot(ANCHOR, { anchor: "eyes", along: "look", reach: tuned("radius"), required: { missCooldown: 0.25 } });
            nobody.sound("minecraft:entity.arrow.hit", { at: "spot", world: true });
        });
        firing.then("reeling");
    });

    component.phase("reeling", reeling => {
        reeling.use("thematic:equip", { item: GUN, slot: "mainhand", displaced: "stash", keep: true });
        reeling.lasts(tuned("reel_for"));
        reeling.until({ not: { fromSpot: { key: ANCHOR, within: 1000 } } }, "cooldown", { check: "before" });
        reeling.until({ fromSpot: { key: ANCHOR, within: tuned("reel_stop_within") } }, "cooldown");
        reelSelf(reeling);
        reeling.every(every => every.push("self", { direction: "toward_spot", strength: tuned("reel_speed"), mode: "set" }));
        reeling.every(0.25, line => line.vfx("thematic:harpoon_beam", { from: "right_hand", to: "spot" }));
        reeling.onExit(exit => exit.particles("minecraft:bubble", { count: 20, spread: 0.4, speed: 0.1 }));
    });
});
