package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.suit.SuitAbilities;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class HeroSlots {
    public static final int SLOTS = 10;

    private HeroSlots() {
    }

    public static void pad(List<Power> powers) {
        while (powers.size() < SLOTS) {
            powers.add(new Power());
        }
    }

    public static boolean arrange(PlayerData data) {
        List<Power> active = data.getActivePowers();
        List<Power> layout = new ArrayList<>();
        List<Power> suit = new ArrayList<>();
        for (Power power : active) {
            if (empty(power)) {
                continue;
            }
            (SuitAbilities.isSuitAbility(power.getName()) ? suit : layout).add(power);
        }
        Iterator<Power> available = data.getAvailablePowers().iterator();
        while (available.hasNext()) {
            Power power = available.next();
            if (!empty(power) && SuitAbilities.isSuitAbility(power.getName())) {
                suit.add(power);
                available.remove();
            }
        }
        layout.addAll(suit);
        while (layout.size() > SLOTS) {
            data.addAvailablePowers(layout.remove(layout.size() - 1));
        }
        pad(layout);
        if (same(active, layout)) {
            return false;
        }
        data.setActivePowers(layout);
        return true;
    }

    private static boolean empty(Power power) {
        return power == null || "none".equals(power.getName());
    }

    private static boolean same(List<Power> a, List<Power> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            String left = empty(a.get(i)) ? "none" : a.get(i).getName();
            String right = empty(b.get(i)) ? "none" : b.get(i).getName();
            if (!left.equals(right)) {
                return false;
            }
        }
        return true;
    }
}
