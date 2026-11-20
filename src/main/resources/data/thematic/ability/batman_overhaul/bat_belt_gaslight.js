component(component => {
    component.traits({ wheelIcon: "thematic:bat_belt" });
    component.press(press => press.then("ready"));
    component.wheel({ color: "#1C1C22", accent: "#F2C318" }, wheel => {
        wheel.entry("batarang", { icon: "thematic:gadget_batarang", label: "Batarang" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: true });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_gas_lantern", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Batarang", { who: "self", color: "yellow" });
        });

        wheel.entry("grapnel", { icon: "thematic:grapplegun", label: "Grapnel" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: true });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_gas_lantern", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Grapnel", { who: "self", color: "yellow" });
        });

        wheel.entry("smoke_pellet", { icon: "thematic:gadget_smoke_pellet", label: "Smoke Pellet" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: true });
            pick.setFlag("bat_gadget_selected_gas_lantern", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Smoke Pellet", { who: "self", color: "yellow" });
        });

        wheel.entry("gas_lantern", { icon: "minecraft:lantern", label: "Gas Lantern" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_gas_lantern", { value: true });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Gas Lantern", { who: "self", color: "yellow" });
        });
    });
});
