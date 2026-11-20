component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:gadget_emp_grenade" });

    component.press(press => {
        press.targets("aimed", { range: tuned("radius"), maxAngle: 6, assist: 0.5, lineOfSight: true, required: true }, target => {
            target.vfx("thematic:fork_strike_low", { from: "main_hand", to: "target_body" });
            target.vfx("thematic:fork_burst", { anchor: "body" });
            target.sound("scguns:item.rusty_gnat.copper_jam", { at: "target", volume: 1.4, pitch: 1, world: true });
            target.sound("minecraft:block.beacon.deactivate", { at: "target", volume: 0.8, pitch: 1.8, world: true });
            target.particles("minecraft:electric_spark", { anchor: "body", count: 18, spread: 0.4, speed: 0.15 });
            target.effect("integrated_superheroes:jammed", { duration: tuned("duration"), amplifier: 0, showParticles: false });
            target.effect("thematic:disable", { duration: tuned("duration"), amplifier: 0, showParticles: false });
            target.effect("minecraft:slowness", { duration: tuned("slowness_duration"), amplifier: tuned("slowness_amplifier"), showParticles: false });
            target.damage({ damage: tuned("damage"), source: "player_attack" });
        });
        press.animation("web_shoot_right");
        press.sound("thematic:laser_charge", { volume: 0.7, pitch: 1.6, world: true });
        press.particles("minecraft:electric_spark", { from: "main_hand", count: 8, spread: 0.1, speed: 0.08 });
    });
});
