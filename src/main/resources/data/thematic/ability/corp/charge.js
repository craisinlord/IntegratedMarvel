corps().forEach(corp => component(`${corp.prefix}_charge`, component => {
    const key = `${corp.prefix}_ringCharge`;
    const shown = corp.prefix.substring(0, 1).toUpperCase() + corp.prefix.substring(1);

    component.meter({
        key: key,
        maxKey: `${corp.prefix}_ringMaxCharge`,
        max: tuned("meter_max"),
        reserve: tuned("meter_reserve"),
        persistent: true,
        regen: { amount: 0, every: 1 },
        hud: { bar: corp.bar, text: `%s ${shown}` },
    });

    component.on("suit_removed", removed => removed.resetConstructs());
}));
