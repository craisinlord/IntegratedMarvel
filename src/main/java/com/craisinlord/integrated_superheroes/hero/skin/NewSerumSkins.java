package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.hero.AmazonPower;
import com.craisinlord.integrated_superheroes.hero.AtlanteanPower;
import com.craisinlord.integrated_superheroes.hero.KryptonianPower;
import com.craisinlord.integrated_superheroes.hero.MartianPower;
import com.craisinlord.integrated_superheroes.hero.ShazamPower;
import com.craisinlord.integrated_superheroes.hero.SonicPower;

import java.util.Map;
import java.util.Set;

public final class NewSerumSkins {
    public static final Map<String, String> COSTUMES = Map.ofEntries(
            Map.entry("supergirl", "thematic:supergirl"),
            Map.entry("superboy", "thematic:superboy"),
            Map.entry("jon_kent", "thematic:jon_kent"),
            Map.entry("cyborg_superman", "thematic:cyborg_superman"),
            Map.entry("bizarro", "thematic:bizarro"),
            Map.entry("miss_martian", "thematic:miss_martian"),
            Map.entry("mary_marvel", "thematic:mary_shazam"),
            Map.entry("black_adam", "thematic:black_adam"),
            Map.entry("darla", "thematic:darla_shazam"),
            Map.entry("eugene", "thematic:eugene_shazam"),
            Map.entry("freddy", "thematic:freddy_shazam"),
            Map.entry("pedro", "thematic:pedro_shazam"),
            Map.entry("mera", "thematic:mera"),
            Map.entry("ocean_master", "thematic:ocean_master"),
            Map.entry("aqualad", "thematic:aqualad"),
            Map.entry("the_deep", "thematic:the_deep"),
            Map.entry("wonder_girl", "thematic:wonder_girl"),
            Map.entry("queen_maeve", "thematic:maeve"),
            Map.entry("big_barda", "thematic:barda"),
            Map.entry("silver_banshee", "thematic:silver_banshee")
    );

    private NewSerumSkins() {
    }

    public static void registerAll() {
        skin("supergirl", KryptonianPower.ID, 0xFF6A2A, 0x2F6BFF);
        skin("power_girl", KryptonianPower.ID, 0xFF5A1E, 0xF2F2F2);
        skin("superboy", KryptonianPower.ID, 0xFF4A1A, 0x1A1A1A);
        skin("jon_kent", KryptonianPower.ID, 0xFF7A2A, 0x2F6BFF);
        skin("cyborg_superman", KryptonianPower.ID, 0xFF6A1A, 0xA8A8A8);
        skin("bizarro", KryptonianPower.ID, 0x9FE6FF, 0x6A7F8A, "inverted");

        skin("miss_martian", MartianPower.ID, 0x6AFF8A, 0xE0262B);

        skin("mary_marvel", ShazamPower.ID, 0xFFF4B0, 0xF2F2F2);
        skin("black_adam", ShazamPower.ID, 0xFFD21F, 0x1A1A1A);
        skin("darla", ShazamPower.ID, 0xFFE14A, 0xFF9FD0);
        skin("eugene", ShazamPower.ID, 0xFFE14A, 0x3A8A3A);
        skin("freddy", ShazamPower.ID, 0xFFE14A, 0x2F4BC4);
        skin("pedro", ShazamPower.ID, 0xFFE14A, 0x2A9A9A);

        skin("mera", AtlanteanPower.ID, 0x3AE08A, 0xE0402A);
        skin("ocean_master", AtlanteanPower.ID, 0x7A3FC4, 0x2A8A6A);
        skin("aqualad", AtlanteanPower.ID, 0x3A8AFF, 0x1A1A2A);
        skin("the_deep", AtlanteanPower.ID, 0x2A6A8A, 0x9FE6C8);

        skin("wonder_girl", AmazonPower.ID, 0xE0262B, 0xFFD21F);
        skin("queen_maeve", AmazonPower.ID, 0xC8C8C8, 0x8A6A3A);
        skin("big_barda", AmazonPower.ID, 0x2F6BFF, 0xFFD21F);

        skin("silver_banshee", SonicPower.ID, 0xE6E6F0, 0x2A2A2A);
    }

    private static void skin(String id, String powerId, int primary, int secondary, String... traits) {
        SerumSkins.register(new SerumSkin(id, powerId, "integrated_superheroes.skin." + id, primary, secondary, Set.of(traits)));
    }
}
