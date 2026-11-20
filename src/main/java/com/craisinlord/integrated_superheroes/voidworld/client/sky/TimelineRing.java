package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class TimelineRing {
    static final float STEP = 0.045F;
    static final int PRUNE_NONE = 0;
    static final int PRUNE_WARN = 1;
    static final int PRUNE_SCAR = 2;
    static final int KIND_SPINE = 0;
    static final int KIND_BRAID = 1;
    static final int KIND_LOOP = 2;
    static final int KIND_FRAY = 3;
    private static final int SPROUT_HIDDEN = 0;
    private static final int SPROUT_GROWING = 1;
    private static final int SPROUT_ALIVE = 2;
    private static final int SPROUT_WITHERING = 3;
    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    static final float CENTER_ELEVATION = (float) Math.toRadians(72.0D);
    static final float RADIUS = (float) Math.toRadians(62.0D);
    private static final float SPIN = TWO_PI / 1500.0F;
    private static final float BAND = 0.034F;
    private static final int STRANDS = 7;
    private static final int ARCS = 8;
    private static final int LOOPS = 11;
    private static final int FRAYS = 16;
    private static final int SPROUTS = 18;
    private static final float CX = 0.0F;
    private static final float CY = (float) Math.sin(CENTER_ELEVATION);
    private static final float CZ = -(float) Math.cos(CENTER_ELEVATION);
    private static final float VY = CZ;
    private static final float VZ = -CY;

    final Branch[] branches;
    final int maxSegs;
    private final Random random = new Random();
    private double revealStart;
    private double nextSprout;
    private float chaos = 0.25F;
    private float px;
    private float py;
    private float pz;
    private float tx;
    private float ty;
    private float tz;

    static final class Branch {
        final int index;
        final int parent;
        final int depth;
        final int kind;
        final float attach;
        final float angle;
        final float length;
        final float curl;
        final boolean sprout;
        final float rank;
        final float delay;
        final float reveal;
        final int strand;
        final float phi0;
        final float span;
        final float braidPhase;
        final int braidFreq;
        final float braidSpeed;
        final float loopAmp;
        final float swayAmp;
        final float swayFreq;
        final float swayPhase;
        final float breatheFreq;
        final float breathePhase;
        final float threadPhase;
        final float width;
        final float glow;
        final float intensity;
        final float taper;
        final int threads;
        final int segs;
        final float[] pts;
        int[] children = new int[0];
        float visible;
        float fade = 1.0F;
        int sproutState;
        double stateStart;
        double stateEnd;
        int prune;
        float cutAt = 1.0F;
        double cutTime;
        float regrow = 1.0F;

        private Branch(Builder b, int index, Random r) {
            this.index = index;
            this.parent = b.parent;
            this.depth = b.depth;
            this.kind = b.kind;
            this.attach = b.attach;
            this.angle = b.angle;
            this.length = b.length;
            this.curl = b.curl;
            this.sprout = b.sprout;
            this.rank = b.rank;
            this.delay = b.delay;
            this.reveal = b.reveal;
            this.strand = b.strand;
            this.phi0 = b.phi0;
            this.span = b.span;
            this.braidPhase = b.braidPhase;
            this.braidFreq = b.braidFreq;
            this.braidSpeed = b.braidSpeed;
            this.loopAmp = b.loopAmp;
            this.width = b.width;
            this.glow = b.glow;
            this.intensity = b.intensity;
            this.taper = b.taper;
            this.threads = b.threads;
            this.swayAmp = 0.012F + r.nextFloat() * 0.025F;
            this.swayFreq = TWO_PI / (35.0F + r.nextFloat() * 45.0F);
            this.swayPhase = r.nextFloat() * TWO_PI;
            this.breatheFreq = TWO_PI / (120.0F + r.nextFloat() * 150.0F);
            this.breathePhase = r.nextFloat() * TWO_PI;
            this.threadPhase = r.nextFloat() * TWO_PI;
            this.segs = Math.max(4, (int) Math.ceil(this.length / STEP));
            this.pts = new float[(this.segs + 1) * 3];
            if (this.sprout) {
                this.visible = 0.0F;
                this.fade = 0.0F;
            }
        }

        boolean ring() {
            return this.kind == KIND_SPINE || this.kind == KIND_BRAID;
        }

        boolean analytic() {
            return this.ring() || this.kind == KIND_LOOP;
        }

        float visibleIndex() {
            return this.visible * this.segs;
        }
    }

    private static final class Builder {
        int parent = -1;
        int depth;
        int kind;
        float attach;
        float angle;
        float length;
        float curl;
        boolean sprout;
        float rank;
        float delay;
        float reveal = 4.0F;
        int strand;
        float phi0;
        float span;
        float braidPhase;
        int braidFreq;
        float braidSpeed;
        float loopAmp;
        float width = 1.0F;
        float glow = 1.0F;
        float intensity = 1.0F;
        float taper = 0.55F;
        int threads = 1;
    }

    TimelineRing(long seed) {
        Random r = new Random(seed);
        List<Branch> list = new ArrayList<>();
        float arcSpan = TWO_PI / ARCS;
        float arcLength = arcSpan * (float) Math.sin(RADIUS);
        int[] spineArcs = new int[ARCS];
        for (int s = 0; s < STRANDS; s++) {
            boolean spine = s == 0;
            float phase = s * TWO_PI / STRANDS + r.nextFloat() * 0.6F;
            int freq = 5 + r.nextInt(5);
            float speed = (0.04F + r.nextFloat() * 0.05F) * (r.nextBoolean() ? 1.0F : -1.0F);
            for (int k = 0; k < ARCS; k++) {
                Builder b = new Builder();
                b.kind = spine ? KIND_SPINE : KIND_BRAID;
                b.strand = s;
                b.phi0 = k * arcSpan;
                b.span = arcSpan;
                b.length = arcLength;
                b.braidPhase = phase;
                b.braidFreq = freq;
                b.braidSpeed = speed;
                b.delay = 0.6F * k + 0.18F * s;
                b.reveal = 0.8F;
                b.taper = 0.0F;
                b.width = spine ? 1.0F : 0.55F;
                b.glow = spine ? 1.0F : 0.4F;
                b.intensity = spine ? 1.0F : 0.72F;
                if (spine) {
                    spineArcs[k] = list.size();
                }
                list.add(new Branch(b, list.size(), r));
            }
        }
        int ringEnd = list.size();
        for (int i = 0; i < LOOPS; i++) {
            Builder b = new Builder();
            float phi = r.nextFloat() * TWO_PI;
            int arc = Math.min(ARCS - 1, (int) (phi / arcSpan));
            b.kind = KIND_LOOP;
            b.depth = 1;
            b.parent = spineArcs[arc];
            b.attach = (phi - arc * arcSpan) / arcSpan;
            b.phi0 = phi;
            b.span = 0.22F + r.nextFloat() * 0.5F;
            b.length = b.span * (float) Math.sin(RADIUS);
            b.loopAmp = (0.035F + r.nextFloat() * 0.09F) * (r.nextBoolean() ? 1.0F : -1.0F);
            b.rank = r.nextFloat() * 0.75F;
            b.taper = 0.0F;
            b.width = 0.5F;
            b.glow = 0.7F;
            b.intensity = 0.82F;
            list.add(new Branch(b, list.size(), r));
        }
        int loopEnd = list.size();
        for (int i = 0; i < FRAYS; i++) {
            Builder b = new Builder();
            float side = r.nextBoolean() ? 1.0F : -1.0F;
            b.kind = KIND_FRAY;
            b.depth = 1;
            b.parent = r.nextInt(3) == 0 ? r.nextInt(ringEnd) : spineArcs[r.nextInt(ARCS)];
            b.attach = 0.05F + r.nextFloat() * 0.85F;
            b.angle = side * (0.08F + r.nextFloat() * 0.22F);
            b.length = 0.3F + r.nextFloat() * 0.5F;
            b.curl = side * (0.15F + r.nextFloat() * 0.4F);
            b.rank = 0.1F + r.nextFloat() * 0.9F;
            b.width = 0.6F;
            b.glow = 0.8F;
            b.intensity = 0.8F;
            b.threads = 2;
            list.add(new Branch(b, list.size(), r));
        }
        int frayEnd = list.size();
        for (int p = loopEnd; p < frayEnd; p++) {
            Branch parent = list.get(p);
            int count = 2 + r.nextInt(2);
            for (int k = 0; k < count; k++) {
                Builder b = new Builder();
                float side = r.nextBoolean() ? 1.0F : -1.0F;
                b.kind = KIND_FRAY;
                b.depth = 2;
                b.parent = p;
                b.attach = 0.45F + r.nextFloat() * 0.4F;
                b.angle = side * (0.04F + r.nextFloat() * 0.14F);
                b.length = parent.length * (0.35F + r.nextFloat() * 0.25F);
                b.curl = side * (0.1F + r.nextFloat() * 0.3F);
                b.rank = Math.min(1.0F, parent.rank + r.nextFloat() * 0.15F);
                b.width = 0.36F;
                b.glow = 0.6F;
                b.intensity = 0.62F;
                list.add(new Branch(b, list.size(), r));
            }
        }
        int childEnd = list.size();
        for (int i = 0; i < SPROUTS; i++) {
            Builder b = new Builder();
            int p = ringEnd + r.nextInt(childEnd - ringEnd);
            Branch parent = list.get(p);
            float side = r.nextBoolean() ? 1.0F : -1.0F;
            b.kind = KIND_FRAY;
            b.depth = parent.depth + 1;
            b.parent = p;
            b.attach = 0.3F + r.nextFloat() * 0.5F;
            b.angle = side * (0.15F + r.nextFloat() * 0.3F);
            b.length = 0.08F + r.nextFloat() * 0.14F;
            b.curl = side * 0.3F * r.nextFloat();
            b.sprout = true;
            b.width = 0.26F;
            b.glow = 0.5F;
            b.intensity = 0.5F;
            list.add(new Branch(b, list.size(), r));
        }
        this.branches = list.toArray(new Branch[0]);
        int[] counts = new int[this.branches.length];
        int most = 0;
        for (Branch b : this.branches) {
            if (b.parent >= 0) {
                counts[b.parent]++;
            }
            most = Math.max(most, b.segs);
        }
        this.maxSegs = most;
        for (Branch b : this.branches) {
            b.children = new int[counts[b.index]];
            counts[b.index] = 0;
        }
        for (Branch b : this.branches) {
            if (b.parent >= 0) {
                this.branches[b.parent].children[counts[b.parent]++] = b.index;
            }
        }
    }

    void reset(double now) {
        this.revealStart = now;
        this.nextSprout = now + 30.0D;
        int seeded = 0;
        for (Branch b : this.branches) {
            b.prune = PRUNE_NONE;
            b.cutAt = 1.0F;
            if (b.sprout) {
                b.sproutState = SPROUT_HIDDEN;
                b.visible = 0.0F;
                b.fade = 0.0F;
                if (seeded < 5 && this.random.nextInt(3) == 0) {
                    b.sproutState = SPROUT_GROWING;
                    b.stateStart = now + 10.0D + seeded * 9.0D;
                    b.stateEnd = b.stateStart + 40.0D + this.random.nextFloat() * 40.0D;
                    seeded++;
                }
            }
        }
    }

    boolean revealing(double now) {
        return now - this.revealStart < 14.0D;
    }

    boolean isAncestor(int ancestor, Branch b) {
        int index = b.parent;
        while (index >= 0) {
            if (index == ancestor) {
                return true;
            }
            index = this.branches[index].parent;
        }
        return false;
    }

    void update(double now, float chaosLevel) {
        this.chaos = chaosLevel;
        if (now >= this.nextSprout) {
            this.nextSprout = now + 25.0D + this.random.nextFloat() * 50.0D;
            this.trySprout(now);
        }
        for (Branch b : this.branches) {
            float delay = b.ring() ? b.delay : 4.0F + b.depth * 1.3F;
            float own = smooth((float) ((now - this.revealStart - delay) / b.reveal));
            if (!b.analytic()) {
                own *= 0.94F + 0.06F * (float) Math.sin(now * b.breatheFreq + b.breathePhase);
            }
            if (b.sprout) {
                own *= this.sproutGrowth(b, now);
            } else if (!b.ring()) {
                own *= smooth((this.chaos - b.rank) / 0.12F);
            }
            if (b.prune == PRUNE_SCAR) {
                float t = (float) ((now - b.cutTime) / b.regrow);
                if (t >= 1.0F) {
                    b.prune = PRUNE_NONE;
                    b.cutAt = 1.0F;
                } else {
                    own = Math.min(own, b.cutAt + (1.0F - b.cutAt) * smooth(t));
                }
            }
            if (b.parent >= 0) {
                Branch parent = this.branches[b.parent];
                float gate = (parent.visible - b.attach) / 0.12F;
                own *= Math.max(0.0F, Math.min(1.0F, gate));
                if (!b.sprout) {
                    b.fade = parent.fade;
                }
            }
            b.visible = own;
        }
    }

    float scarGlow(Branch b, double now) {
        if (b.prune != PRUNE_SCAR) {
            return 0.0F;
        }
        float t = (float) ((now - b.cutTime) / b.regrow);
        float left = Math.max(0.0F, 1.0F - t);
        return left * left;
    }

    private float sproutGrowth(Branch b, double now) {
        switch (b.sproutState) {
            case SPROUT_GROWING -> {
                float t = (float) ((now - b.stateStart) / (b.stateEnd - b.stateStart));
                b.fade = 1.0F;
                if (t >= 1.0F) {
                    b.sproutState = SPROUT_ALIVE;
                    b.stateStart = now;
                    b.stateEnd = now + 120.0D + this.random.nextFloat() * 180.0D;
                    return 1.0F;
                }
                return smooth(t);
            }
            case SPROUT_ALIVE -> {
                b.fade = 1.0F;
                if (now >= b.stateEnd) {
                    b.sproutState = SPROUT_WITHERING;
                    b.stateStart = now;
                    b.stateEnd = now + 25.0D;
                }
                return 1.0F;
            }
            case SPROUT_WITHERING -> {
                float t = (float) ((now - b.stateStart) / (b.stateEnd - b.stateStart));
                if (t >= 1.0F) {
                    b.sproutState = SPROUT_HIDDEN;
                    b.fade = 0.0F;
                    return 0.0F;
                }
                b.fade = 1.0F - t;
                return 1.0F;
            }
            default -> {
                b.fade = 0.0F;
                return 0.0F;
            }
        }
    }

    private void trySprout(double now) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Branch b = this.branches[this.branches.length - SPROUTS + this.random.nextInt(SPROUTS)];
            if (b.sproutState != SPROUT_HIDDEN || b.prune != PRUNE_NONE) {
                continue;
            }
            Branch parent = this.branches[b.parent];
            if (parent.visible < b.attach + 0.1F || parent.prune != PRUNE_NONE) {
                continue;
            }
            b.sproutState = SPROUT_GROWING;
            b.stateStart = now;
            b.stateEnd = now + 40.0D + this.random.nextFloat() * 50.0D;
            return;
        }
    }

    void cut(Branch b, double now, float regrowSeconds) {
        b.prune = PRUNE_SCAR;
        b.cutTime = now;
        b.regrow = regrowSeconds;
    }

    boolean prunable(Branch b) {
        if (b.depth < 1 || b.prune != PRUNE_NONE) {
            return false;
        }
        int index = b.parent;
        while (index >= 0) {
            Branch up = this.branches[index];
            if (up.prune == PRUNE_WARN) {
                return false;
            }
            index = up.parent;
        }
        for (Branch other : this.branches) {
            if (other.prune == PRUNE_WARN && this.isAncestor(b.index, other)) {
                return false;
            }
        }
        return true;
    }

    private float centreline(float phi, double now) {
        float wobble = 0.004F + 0.03F * this.chaos * this.chaos;
        return RADIUS + wobble * ((float) Math.sin(2.0F * phi + now * 0.031D) * 0.6F + (float) Math.sin(5.0F * phi - now * 0.053D + 1.3D) * 0.4F);
    }

    private static float pinch(float phi) {
        return 0.3F + 0.7F * (0.5F + 0.5F * (float) Math.sin(3.0F * phi + 0.7F));
    }

    private static void point(float[] out, int o, float phi, float rho) {
        float cr = (float) Math.cos(rho);
        float sr = (float) Math.sin(rho);
        float cp = (float) Math.cos(phi);
        float sp = (float) Math.sin(phi);
        out[o] = CX * cr + cp * sr;
        out[o + 1] = CY * cr + sp * VY * sr;
        out[o + 2] = CZ * cr + sp * VZ * sr;
    }

    private void solveAnalytic(Branch b, double now) {
        float spin = (float) (now * SPIN % TWO_PI);
        float[] out = b.pts;
        for (int i = 0; i <= b.segs; i++) {
            float t = (float) i / b.segs;
            float phi = b.phi0 + t * b.span;
            float rho = this.centreline(phi, now);
            if (b.kind == KIND_BRAID) {
                float spread = BAND * pinch(phi) * (1.0F + 0.6F * this.chaos);
                rho += spread * (float) Math.sin(b.braidFreq * phi + b.braidPhase + now * b.braidSpeed);
            } else if (b.kind == KIND_LOOP) {
                float amp = b.loopAmp * (1.0F + 0.15F * (float) Math.sin(now * b.swayFreq + b.swayPhase)) * (0.7F + 0.6F * this.chaos);
                float bulge = (float) Math.sin(Math.PI * t);
                rho += amp * bulge * bulge * (3.0F - 2.0F * bulge);
            }
            point(out, i * 3, phi + spin, rho);
        }
    }

    void solve(double now, float dx, float dy, float dz, float bend) {
        for (Branch b : this.branches) {
            if (b.analytic()) {
                this.solveAnalytic(b, now);
                continue;
            }
            Branch parent = this.branches[b.parent];
            float fi = b.attach * parent.segs;
            int i = Math.min((int) fi, parent.segs - 1);
            float f = fi - i;
            float[] p = parent.pts;
            int a = i * 3;
            int c = a + 3;
            this.px = p[a] + (p[c] - p[a]) * f;
            this.py = p[a + 1] + (p[c + 1] - p[a + 1]) * f;
            this.pz = p[a + 2] + (p[c + 2] - p[a + 2]) * f;
            this.normalizeP();
            this.tx = p[c] - p[a];
            this.ty = p[c + 1] - p[a + 1];
            this.tz = p[c + 2] - p[a + 2];
            this.orthogonalizeT();
            this.rotateT(b.angle + b.swayAmp * (float) Math.sin(now * b.swayFreq + b.swayPhase));
            float ds = b.length / b.segs;
            float[] out = b.pts;
            out[0] = this.px;
            out[1] = this.py;
            out[2] = this.pz;
            for (int k = 1; k <= b.segs; k++) {
                this.rotateT(b.curl * ds);
                this.px += this.tx * ds;
                this.py += this.ty * ds;
                this.pz += this.tz * ds;
                this.normalizeP();
                this.orthogonalizeT();
                out[k * 3] = this.px;
                out[k * 3 + 1] = this.py;
                out[k * 3 + 2] = this.pz;
            }
        }
        if (bend <= 0.0F) {
            return;
        }
        for (Branch b : this.branches) {
            float[] p = b.pts;
            for (int i = 0; i < p.length; i += 3) {
                float d = p[i] * dx + p[i + 1] * dy + p[i + 2] * dz;
                if (d <= 0.0F) {
                    continue;
                }
                float d2 = d * d;
                float w = bend * 0.5F * d2 * d2;
                float x = p[i] + (dx - p[i] * d) * w;
                float y = p[i + 1] + (dy - p[i + 1] * d) * w;
                float z = p[i + 2] + (dz - p[i + 2] * d) * w;
                float len = (float) Math.sqrt(x * x + y * y + z * z);
                p[i] = x / len;
                p[i + 1] = y / len;
                p[i + 2] = z / len;
            }
        }
    }

    private void rotateT(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float cx = this.py * this.tz - this.pz * this.ty;
        float cy = this.pz * this.tx - this.px * this.tz;
        float cz = this.px * this.ty - this.py * this.tx;
        this.tx = this.tx * cos + cx * sin;
        this.ty = this.ty * cos + cy * sin;
        this.tz = this.tz * cos + cz * sin;
    }

    private void normalizeP() {
        float len = (float) Math.sqrt(this.px * this.px + this.py * this.py + this.pz * this.pz);
        if (len > 1.0E-6F) {
            this.px /= len;
            this.py /= len;
            this.pz /= len;
        }
    }

    private void orthogonalizeT() {
        float d = this.tx * this.px + this.ty * this.py + this.tz * this.pz;
        this.tx -= this.px * d;
        this.ty -= this.py * d;
        this.tz -= this.pz * d;
        this.normalizeT();
    }

    private void normalizeT() {
        float len = (float) Math.sqrt(this.tx * this.tx + this.ty * this.ty + this.tz * this.tz);
        if (len > 1.0E-6F) {
            this.tx /= len;
            this.ty /= len;
            this.tz /= len;
        } else {
            this.tx = 1.0F;
            this.ty = 0.0F;
            this.tz = 0.0F;
        }
    }

    static float smooth(float t) {
        if (t <= 0.0F) {
            return 0.0F;
        }
        if (t >= 1.0F) {
            return 1.0F;
        }
        return t * t * (3.0F - 2.0F * t);
    }
}
