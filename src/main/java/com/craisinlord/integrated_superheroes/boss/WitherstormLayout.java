package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.anthology.multipart.MultipartLayout;
import com.craisinlord.anthology.multipart.MultipartPartDefinition;

public final class WitherstormLayout implements MultipartLayout {
    public static final int CORE = 0;
    public static final int HEAD_CENTER = 1;
    public static final int HEAD_LEFT = 2;
    public static final int HEAD_RIGHT = 3;
    public static final int TORSO = 4;
    public static final int HEAD_COUNT = 3;

    private static final WitherstormLayout[] STAGES = new WitherstormLayout[WitherstormEntity.MAX_STAGE + 1];

    private final MultipartPartDefinition[] parts;

    private WitherstormLayout(MultipartPartDefinition[] parts) {
        this.parts = parts;
    }

    public static WitherstormLayout forStage(int stage) {
        int clamped = Math.clamp(stage, 1, WitherstormEntity.MAX_STAGE);
        WitherstormLayout layout = STAGES[clamped];
        if (layout == null) {
            float scale = WitherstormEntity.renderScale(clamped);
            layout = clamped == 1 ? small(scale) : big(scale);
            STAGES[clamped] = layout;
        }
        return layout;
    }

    private static WitherstormLayout small(float s) {
        float u = s / 16.0F;
        return new WitherstormLayout(new MultipartPartDefinition[]{
                part("core", 9.0F * u, 9.0F * u, 4.5F * u, 7.5F * u, 0.0F),
                part("head_center", 8.5F * u, 8.5F * u, 0.0F, 19.75F * u, 0.0F),
                part("head_left", 6.5F * u, 6.5F * u, 1.0F * u, 17.75F * u, 8.5F * u),
                part("head_right", 6.5F * u, 6.5F * u, 1.0F * u, 17.75F * u, -8.5F * u),
                part("torso", 5.0F * u, 16.0F * u, -1.0F * u, 1.0F * u, 0.0F)
        });
    }

    private static WitherstormLayout big(float s) {
        return new WitherstormLayout(new MultipartPartDefinition[]{
                part("core", 4.0F * s, 4.0F * s, 1.5F * s, 6.1F * s, 0.0F),
                part("head_center", 3.5F * s, 3.5F * s, 4.4F * s, 7.4F * s, 0.1F * s),
                part("head_left", 3.0F * s, 3.0F * s, 6.0F * s, 11.5F * s, 4.0F * s),
                part("head_right", 3.0F * s, 3.0F * s, 5.9F * s, 10.2F * s, -4.25F * s),
                part("torso", 8.0F * s, 10.0F * s, -3.0F * s, 1.0F * s, 0.0F)
        });
    }

    private static MultipartPartDefinition part(String name, float width, float height, float forward, float y, float lateral) {
        return new MultipartPartDefinition(name, width, height, 1.0F, forward, y, lateral, false);
    }

    @Override
    public MultipartPartDefinition[] parts() {
        return parts;
    }
}
