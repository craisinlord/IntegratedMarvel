const BAT = "thematic:bat";

component(component => {
    component.cooldown();

    component.on("use_entity", { hand: "main", morphedAs: BAT, ready: true }, use => {
        use.targets("victim", { hittable: true }, bitten => {
            bitten.damage({ source: "player_attack" });
            bitten.effect("thematic:bleed", { duration: tuned("bleed_duration"), amplifier: expr("clamp(amplifier, 1, 3)") });
            bitten.action("integrated_superheroes:dracula_drink", {});
            bitten.decal("thematic:blood_splatter[down=true]", { lanes: 2, spacing: 1, depth: 2, max: 8, lasts: 20 });
            bitten.particles("thematic:blood_particle", { count: 18, spread: 0.25, speed: 0.03, anchor: 0.85 });
            bitten.sound("minecraft:entity.bat.hurt", { pitch: 0.6, world: true });
            bitten.sound("minecraft:entity.generic.drink", { pitch: 0.6, world: true });
            bitten.startCooldown();
        });
        use.cancelEvent();
    });
});
