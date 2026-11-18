package com.system;

import java.io.Serializable;
import java.util.Random;

/** Gerador com estado explícito para continuar os mesmos sorteios após um load. */
public final class GameRandom implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long MASK = (1L << 48) - 1;
    private long state;

    public GameRandom() {
        this(new Random().nextLong());
    }

    public GameRandom(long seed) {
        state = (seed ^ MULTIPLIER) & MASK;
    }

    public static GameRandom fromState(long state) {
        if (state < 0 || state > MASK) {
            throw new IllegalArgumentException("Estado aleatório inválido");
        }
        GameRandom random = new GameRandom(0);
        random.state = state;
        return random;
    }

    public long getState() {
        return state;
    }

    private int next(int bits) {
        state = (state * MULTIPLIER + 0xBL) & MASK;
        return (int) (state >>> (48 - bits));
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) / (double) (1L << 53);
    }

    public int nextInt(int origin, int bound) {
        int range = bound - origin;
        if (range <= 0) {
            throw new IllegalArgumentException("Intervalo inválido");
        }
        int bits;
        int value;
        do {
            bits = next(31);
            value = bits % range;
        } while (bits - value + range - 1 < 0);
        return origin + value;
    }
}
