function saberThrow(id, saber, throwable, clip) {
    component(id, component => {
        component.cooldown();
        component.traits({ wheelIcon: saber });
        component.requires({ heldItem: saber }, { message: "integrated_superheroes.sith.saber_needed" });

        component.press(press => {
            press.animation(clip, { fadeIn: 0.05 });
            press.sound("opposing_force:laser_blade_spin", { volume: 1.2, pitch: 0.9, world: true });
            press.projectile({
                body: "thrown",
                throwable: throwable,
                byDefinition: true,
                consumeHeld: saber,
                from: { point: "eyes", offset: [0, -0.2, 0] },
                speed: tuned("projectile_speed"),
            });
        });
    });
}

saberThrow("vader_saber_throw", "thematic:vader_saber", "thematic:vader_saber_thrown", "helmet_throw");
saberThrow("maul_saber_throw", "thematic:maul_saberstaff", "thematic:maul_saberstaff_thrown", "shield_throw");
