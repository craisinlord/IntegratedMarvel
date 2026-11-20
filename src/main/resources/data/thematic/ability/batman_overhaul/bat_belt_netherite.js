component(component => {
    component.traits({ wheelIcon: "thematic:bat_belt" });
    component.press(press => press.then("ready"));
    component.wheel({ color: "#1C1C22", accent: "#F2C318" }, wheel => {
        wheel.entry("netherite_batarang", { icon: "thematic:gadget_netherite_batarang", label: "Netherite Batarang" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: true });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: false });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: false });
            pick.setFlag("bat_gadget_selected_disruptor", { value: false });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Netherite Batarang", { who: "self", color: "yellow" });
        });

        wheel.entry("grapnel", { icon: "thematic:grapplegun", label: "Grapnel" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: true });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: false });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: false });
            pick.setFlag("bat_gadget_selected_disruptor", { value: false });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Grapnel", { who: "self", color: "yellow" });
        });

        wheel.entry("explosive_gel", { icon: "thematic:explosivegelgun", label: "Explosive Gel" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: true });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: false });
            pick.setFlag("bat_gadget_selected_disruptor", { value: false });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Explosive Gel", { who: "self", color: "yellow" });
        });

        wheel.entry("gel_detonate", { icon: "minecraft:lever", label: "Detonate Gel" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: false });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: true });
            pick.setFlag("bat_gadget_selected_disruptor", { value: false });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Detonate Gel", { who: "self", color: "yellow" });
        });

        wheel.entry("disruptor", { icon: "thematic:gadget_emp_grenade", label: "Disruptor" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: false });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: false });
            pick.setFlag("bat_gadget_selected_disruptor", { value: true });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Disruptor", { who: "self", color: "yellow" });
        });

        wheel.entry("electric_batarang", { icon: "thematic:gadget_electric_batarang", label: "Electric Batarang" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: false });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: false });
            pick.setFlag("bat_gadget_selected_disruptor", { value: false });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: true });
            pick.setFlag("bat_gadget_selected_rally", { value: false });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Electric Batarang", { who: "self", color: "yellow" });
        });

        wheel.entry("rally", { icon: "thematic:comms_device", label: "Rally" }, pick => {
            pick.setFlag("bat_gadget_selected_netherite_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_grapnel", { value: false });
            pick.setFlag("bat_gadget_selected_explosive_gel", { value: false });
            pick.setFlag("bat_gadget_selected_gel_detonate", { value: false });
            pick.setFlag("bat_gadget_selected_disruptor", { value: false });
            pick.setFlag("bat_gadget_selected_electric_batarang", { value: false });
            pick.setFlag("bat_gadget_selected_rally", { value: true });
            pick.sound("minecraft:item.armor.equip_leather", { volume: 0.8, pitch: 1.4 });
            pick.notify("Rally", { who: "self", color: "yellow" });
        });
    });
});
