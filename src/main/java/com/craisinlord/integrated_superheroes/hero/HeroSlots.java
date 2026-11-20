package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.suit.SuitAbilities;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class HeroSlots {
    public static final int SLOTS = 10;
    public static final int VILTRUMITE_OFFSET = 4;

    private HeroSlots() {
    }

    public static void pad(List<Power> powers) {
        while (powers.size() < SLOTS) {
            powers.add(new Power());
        }
    }

    public static boolean makeRoom(PlayerData data) {
        if (data.findFirstEmpty() >= 0) {
            return true;
        }
        data.retainActivePowerSize();
        if (data.findFirstEmpty() >= 0) {
            return true;
        }
        List<Power> active = data.getActivePowers();
        for (int i = active.size() - 1; i >= 0; i--) {
            Power power = active.get(i);
            if (SuitAbilities.isSuitAbility(power.getName())) {
                data.addAvailablePowers(power);
                active.set(i, new Power());
                return true;
            }
        }
        return false;
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
        if (ViltrumitePower.ID.equals(data.getPrimaryPower())) {
            for (int i = 0; i < VILTRUMITE_OFFSET; i++) {
                layout.add(0, new Power());
            }
        }
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
