component(component => {
    component.traits({
        wheelIcon: "minecraft:zombie_head",
        damageBonus: {
            times: tuned("damage_bonus_times"),
            when: { target: { entity: "#minecraft:undead" } }
        }
    });
});
