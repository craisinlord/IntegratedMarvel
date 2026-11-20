package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.BinaryPower;
import com.craisinlord.integrated_superheroes.hero.HulkPower;
import com.craisinlord.integrated_superheroes.hero.HumanTorchPower;
import com.craisinlord.integrated_superheroes.hero.KittyPower;
import com.craisinlord.integrated_superheroes.hero.MrFantasticPower;
import com.craisinlord.integrated_superheroes.hero.QuicksilverPower;
import com.craisinlord.integrated_superheroes.hero.ScarletWitchPower;
import com.craisinlord.integrated_superheroes.hero.StormPower;
import com.craisinlord.integrated_superheroes.hero.ViltrumitePower;

import java.util.Map;
import java.util.Set;

public final class ExistingSerumSkins {
    public static final Map<String, String> COSTUMES = Map.ofEntries(
            Map.entry("flash", "thematic:the_flash"),
            Map.entry("reverse_flash", "thematic:reverse_flash"),
            Map.entry("zoom", "thematic:zoom_cw"),
            Map.entry("kid_flash", "thematic:kidflash"),
            Map.entry("jay_garrick", "thematic:jay_garrick"),
            Map.entry("godspeed", "thematic:godspeed"),
            Map.entry("a_train", "thematic:a_train"),
            Map.entry("kabal", "thematic:kabal"),
            Map.entry("plastic_man", "thematic:plastic_man"),
            Map.entry("clayface", "thematic:clayface"),
            Map.entry("invincible", "thematic:invincible"),
            Map.entry("omni_man", "thematic:omni_man"),
            Map.entry("she_hulk", "thematic:she_hulk"),
            Map.entry("red_hulk", "thematic:red_hulk"),
            Map.entry("starfire", "thematic:starfire"),
            Map.entry("captain_atom", "thematic:captain_atom"),
            Map.entry("firestorm", "thematic:firestorm"),
            Map.entry("deadman", "thematic:deadman"),
            Map.entry("gentleman_ghost", "thematic:gentleman_ghost"),
            Map.entry("zatanna", "thematic:zatanna"),
            Map.entry("raven", "thematic:raven"),
            Map.entry("stormfront", "thematic:stormfront"),
            Map.entry("power_girl", "thematic:powergirl")
    );

    private ExistingSerumSkins() {
    }

    public static void registerAll() {
        skin("flash", QuicksilverPower.ID, 0xFFD21F, 0xFFF4B0);
        skin("reverse_flash", QuicksilverPower.ID, 0xFF2A1A, 0xFFD21F);
        skin("zoom", QuicksilverPower.ID, 0x2F6BFF, 0x9CC2FF);
        skin("kid_flash", QuicksilverPower.ID, 0xFFE14A, 0xD4302A);
        skin("jay_garrick", QuicksilverPower.ID, 0xFFC83D, 0xC0C0C0);
        skin("godspeed", QuicksilverPower.ID, 0xF4F4F4, 0xD8B23A);
        skin("a_train", QuicksilverPower.ID, 0x6FA8FF, 0xFFFFFF);
        skin("kabal", QuicksilverPower.ID, 0xC8C8C8, 0xA01818);

        skin("plastic_man", MrFantasticPower.ID, 0xE0262B, 0xFFD21F);
        skin("elongated_man", MrFantasticPower.ID, 0x8A3FA0, 0xE0262B);
        skin("clayface", MrFantasticPower.ID, 0x8B6B47, 0x5E4630);

        if (ModCompat.VILTRUMITE) {
            skin("invincible", ViltrumitePower.ID, 0xFFD300, 0x1E5AA8);
            skin("omni_man", ViltrumitePower.ID, 0xF2F2F2, 0xC8102E);
        }

        skin("she_hulk", HulkPower.ID, 0x7FC77A, 0x9CFF6A);
        skin("red_hulk", HulkPower.ID, 0xD9402E, 0xFF7A1F, RedHulkHeat.TRAIT);

        skin("starfire", BinaryPower.ID, 0x4DFF6A, 0xFF8A1F);
        skin("captain_atom", BinaryPower.ID, 0xD8E6F0, 0xE03030);

        skin("firestorm", HumanTorchPower.ID, 0xFF8A1F, 0xFFE04A);

        skin("deadman", KittyPower.ID, 0xC8102E, 0xF2F2F2);
        skin("gentleman_ghost", KittyPower.ID, 0x9FE6C8, 0x3A3A3A);

        skin("zatanna", ScarletWitchPower.ID, 0x9FC4FF, 0xFFFFFF);
        skin("raven", ScarletWitchPower.ID, 0x6A2BC4, 0x1A0A2E);

        skin("stormfront", StormPower.ID, 0xFF5A3C, 0xFFE8D0);
    }

    private static void skin(String id, String powerId, int primary, int secondary, String... traits) {
        SerumSkins.register(new SerumSkin(id, powerId, "integrated_superheroes.skin." + id, primary, secondary, Set.of(traits)));
    }
}
