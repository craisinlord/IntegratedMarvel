component(component => {
    component.traits({ wheelIcon: "thematic:bat_belt" });
    component.press(press => press.then("ready"));
    component.wheel({ color: "#1C1C22", accent: "#F2C318" }, wheel => {
        wheel.entry("batarang", { icon: "thematic:gadget_batarang", label: "Batarang" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: true });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_sonar_pulse", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Batarang", { who: "self", color: "yellow" });
        });

        wheel.entry("grapnel", { icon: "thematic:grapplegun", label: "Grapnel" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: true });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_sonar_pulse", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Grapnel", { who: "self", color: "yellow" });
        });

        wheel.entry("smoke_pellet", { icon: "thematic:gadget_smoke_pellet", label: "Smoke Pellet" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: true });
            pick.setFlag("bat_gadget_selected_sonar_pulse", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Smoke Pellet", { who: "self", color: "yellow" });
        });

        wheel.entry("sonar_pulse", { icon: "minecraft:heart_of_the_sea", label: "Sonar Pulse" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_sonar_pulse", { value: true });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Sonar Pulse", { who: "self", color: "yellow" });
        });

        wheel.entry("rally", { icon: "thematic:comms_device", label: "Rally" }, pick => {
            pick.setFlag("bat_gadget_selected_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_smoke_pellet", { value: false });
            pick.setFlag("bat_gadget_selected_sonar_pulse", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: true });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Rally", { who: "self", color: "yellow" });
        });
    });
});
