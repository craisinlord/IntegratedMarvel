const ORDER = "integrated_superheroes:batmobile_order";

function orders() {
    return { ram: tuned("damage"), gun: tuned("gun_damage"), push: tuned("push_strength") };
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:bat_signal" });
    component.blockedBy({ riding: true });

    component.press(press => {
        press.animation("gadget_throw");
        press.sound("minecraft:block.beacon.activate", { volume: 0.6, pitch: 1.8, world: true });
        press.particles("minecraft:electric_spark", { from: "left_hand", count: 6, spread: 0.1, speed: 0.05 });
        press.targets("locked_or_look", { range: tuned("radius"), aimAssist: true, lineOfSight: true }, target => {
            target.mark("bat_batmobile_mark", { for: tuned("mark_for"), glow: true, seenBy: "owner", vfx: "thematic:hunt_mark" });
            target.vfx("thematic:hunt_bullseye", { from: "eyes", to: "target_body" });
            target.sound("minecraft:block.note_block.bit", { volume: 0.8, pitch: 0.7, heardBy: "wearer" });
            target.action(ORDER, orders());
        }, nobody => {
            nobody.action(ORDER, orders());
            nobody.sound("minecraft:block.note_block.bit", { volume: 0.8, pitch: 1.4, heardBy: "wearer" });
        });
    });
});
