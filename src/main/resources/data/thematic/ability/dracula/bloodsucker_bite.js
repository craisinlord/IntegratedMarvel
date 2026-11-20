const DRAIN_SECONDS = tuned("drain_seconds");
const KRYPTONIAN = { target: { suitTag: "kryptonian" } };
const STOP = { mode: "set", strength: 0 };

component(component => {
    component.cooldown();
    component.visual("thematic:glow", { model: "geo/armor/dracula_eyes.geo.json", tint: [0.6, 0, 0], alpha: 0.6 });
    component.visual("thematic:screen_vignette", {
        color: 0x000000,
        edge_color: 0x000000,
        strength: 0.85,
        radius: 0.35,
        softness: 0.7,
        fade_in: 0.4,
        fade_out: 1,
        first_person_only: true,
    });

    component.press(press => {
        press.sound("minecraft:entity.bat.ambient", { pitch: 0.6, world: true });
    });

    component.use("thematic:armed", { window: 3 }, hit => {
        hit.cancelEvent();
        hit.targets("victim", { remember: true }, prey => {
            prey.when(KRYPTONIAN, kryptonian => {
                kryptonian.damage({ who: "self", element: "solar" });
                kryptonian.particles("minecraft:flame", { count: 16, spread: 0.2, speed: 0.04, anchor: 0.85 });
                kryptonian.sound("minecraft:entity.generic.burn", { pitch: 0.8, world: true });
                kryptonian.notify("message.thematic.dracula.solar_blood");
            }, mortal => {
                mortal.teleport("in_front_of_owner", { distance: tuned("teleport_distance"), stop: true });
                mortal.lookAt("target", { away: true });
                mortal.lookAt("self", { drop: 0.35 });
                mortal.effect("thematic:immobile", { duration: DRAIN_SECONDS, amplifier: tuned("immobile_amplifier") });
                mortal.selfEffect("thematic:immobile", { duration: DRAIN_SECONDS, amplifier: tuned("immobile_amplifier_2") });
                mortal.lockMoves({ mode: "interrupt", who: "self", for: DRAIN_SECONDS });
                mortal.when({ target: { player: true } }, player => {
                    player.lockMoves({ mode: "interrupt", for: DRAIN_SECONDS });
                });

                mortal.effect("thematic:bleed", { duration: DRAIN_SECONDS, amplifier: expr("clamp(amplifier, 1, 3)") });
                mortal.heal(tuned("heal"));
                mortal.action("integrated_superheroes:dracula_drink", {});
                mortal.effect("minecraft:darkness", { duration: DRAIN_SECONDS, showParticles: false, showIcon: false });
                mortal.decal("thematic:blood_splatter[down=true]", { lanes: 2, spacing: 1, depth: 2, max: 8, lasts: 20 });
                mortal.particles("thematic:blood_particle", { count: 18, spread: 0.25, speed: 0.03, anchor: 0.85 });
                mortal.sound("minecraft:entity.generic.drink", { pitch: 0.6, world: true });
            });
        });
        hit.then("drain");
    });

    component.phase("drain", drain => {
        drain.lasts(DRAIN_SECONDS);
        drain.blocksMovement();
        drain.until({ any: [KRYPTONIAN, { not: { target: { alive: true, sameWorld: true } } }] }, "cooldown", { check: "before" });
        drain.every(every => {
            every.targets("remembered", held => {
                held.push("target", STOP);
                held.push("self", STOP);
                held.lookAt("target", { away: true });
                held.lookAt("self", { drop: 0.35 });
            });
        });
        drain.every(0.5, drink => {
            drink.targets("remembered", held => {
                held.particles("thematic:blood_particle", { count: 6, spread: 0.15, speed: 0.02, anchor: 0.85 });
            });
        });
        drain.every(1, sip => {
            sip.targets("remembered", held => {
                held.action("integrated_superheroes:dracula_drink", {});
            });
        });
        drain.onExit(exit => {
            exit.targets("remembered", held => {
                held.removeEffect("thematic:immobile", { who: "target" });
            });
            exit.removeEffect("thematic:immobile");
        });
    });
});
