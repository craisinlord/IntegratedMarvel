function flagOf(prefix, move) {
    return prefix + "_selected_" + move[0];
}

function useChain(prefix, moves, index, actions) {
    let move = moves[index];
    if (index === moves.length - 1) {
        actions.useMove(move[0], { ready: true });
        return;
    }
    actions.when({ flag: flagOf(prefix, move) }, chosen => {
        chosen.useMove(move[0], { ready: true });
    }, other => {
        useChain(prefix, moves, index + 1, other);
    });
}

function forcePowers(prefix, icon, moves) {
    let ordered = moves.slice(1).concat([moves[0]]);

    component(prefix + "_force_wheel", component => {
        component.traits({ wheelIcon: icon });
        component.press();
        component.wheel({ color: "#1A0505", accent: "#D41414" }, wheel => {
            moves.forEach(move => {
                wheel.entry(move[0], { icon: move[1], label: "ability.thematic." + move[0] }, pick => {
                    moves.forEach(other => {
                        pick.setFlag(flagOf(prefix, other), { value: other[0] === move[0] });
                    });
                    pick.sound("opposing_force:laser_blade_swing", { volume: 0.6, pitch: 1.4 });
                    pick.notify(move[2], { who: "wearer", color: "red" });
                });
            });
        });
    });

    component(prefix + "_force_use", component => {
        component.traits({ wheelIcon: icon });
        component.press(press => {
            useChain(prefix, ordered, 0, press);
        });
    });
}

forcePowers("vader", "minecraft:wither_rose", [
    ["vader_force_choke", "minecraft:wither_rose", "Force Choke"],
    ["vader_force_push", "minecraft:wind_charge", "Force Push"],
    ["vader_force_pull", "minecraft:lead", "Force Pull"],
    ["vader_saber_throw", "thematic:vader_saber", "Saber Throw"],
]);

forcePowers("maul", "minecraft:breeze_rod", [
    ["maul_force_push", "minecraft:breeze_rod", "Force Push"],
    ["maul_force_pull", "minecraft:lead", "Force Pull"],
    ["maul_saber_throw", "thematic:maul_saberstaff", "Saber Throw"],
]);
