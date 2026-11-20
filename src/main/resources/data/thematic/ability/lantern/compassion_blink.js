component("compassion_blink", component => {
    component.cooldown();

    component.press(press => {
        press.particles(dust([0.4, 0.0, 0.8], 1.2), { count: 30, spread: [0.4, 1.0, 0.4], speed: 0.05, yOffset: 1 });
        press.teleport("along_look_clear", { distance: tuned("range") }, blocked => blocked.notify("No room to blink"));
        press.particles(dust([0.4, 0.0, 0.8], 1.2), { count: 30, spread: [0.4, 1.0, 0.4], speed: 0.05, yOffset: 1 });
        press.sound("minecraft:entity.enderman.teleport", { pitch: 1.3 });
    });
});
