component(component => {
    component.traits({ wheelIcon: "thematic:bat_belt" });
    component.press(press => press.then("ready"));
    component.wheel({ color: "#1C1C22", accent: "#F2C318" }, wheel => {
        wheel.entry("improvised", { icon: "thematic:gadget_bola", label: "Improvised Gadget" }, pick => {
            pick.setFlag("bat_gadget_selected_improvised", { value: true });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Improvised Gadget", { who: "self", color: "yellow" });
        });

        wheel.entry("grapnel", { icon: "thematic:grapplegun", label: "Grapnel" }, pick => {
            pick.setFlag("bat_gadget_selected_improvised", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: true });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Grapnel", { who: "self", color: "yellow" });
        });

        wheel.entry("rally", { icon: "thematic:comms_device", label: "Rally" }, pick => {
            pick.setFlag("bat_gadget_selected_improvised", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: true });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Rally", { who: "self", color: "yellow" });
        });
    });
});
