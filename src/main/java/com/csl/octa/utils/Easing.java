package com.csl.octa.utils;

import java.util.function.DoubleUnaryOperator;

@SuppressWarnings("unused")
public enum Easing {
    // LINEAR
    LINEAR(t -> t),

    // SINE
    EASE_IN_SINE(t -> 1 - Math.cos((t * Math.PI) / 2)),
    EASE_OUT_SINE(t -> Math.sin((t * Math.PI) / 2)),
    EASE_INOUT_SINE(t -> -(Math.cos(Math.PI * t) - 1) / 2),

    // QUAD
    EASE_IN_QUAD(t -> t * t),
    EASE_OUT_QUAD(t -> 1 - (1 - t) * (1 - t)),
    EASE_INOUT_QUAD(t -> t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2),

    // CUBIC
    EASE_IN_CUBIC(t -> t * t * t),
    EASE_OUT_CUBIC(t -> 1 - Math.pow(1 - t, 3)),
    EASE_INOUT_CUBIC(t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),

    // QUART
    EASE_IN_QUART(t -> t * t * t * t),
    EASE_OUT_QUART(t -> 1 - Math.pow(1 - t, 4)),
    EASE_INOUT_QUART(t -> t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2),

    // QUINT
    EASE_IN_QUINT(t -> t * t * t * t * t),
    EASE_OUT_QUINT(t -> 1 - Math.pow(1 - t, 5)),
    EASE_INOUT_QUINT(t -> t < 0.5 ? 16 * t * t * t * t * t : 1 - Math.pow(-2 * t + 2, 5) / 2),

    // EXPO
    EASE_IN_EXPO(t -> t == 0 ? 0 : Math.pow(2, 10 * t - 10)),
    EASE_OUT_EXPO(t -> t == 1 ? 1 : 1 - Math.pow(2, -10 * t)),
    EASE_INOUT_EXPO(t -> {
        if (t == 0) return 0.0;
        if (t == 1) return 1.0;
        return t < 0.5
                ? Math.pow(2, 20 * t - 10) / 2
                : (2 - Math.pow(2, -20 * t + 10)) / 2;
    }),

    // CIRC
    EASE_IN_CIRC(t -> 1 - Math.sqrt(1 - t * t)),
    EASE_OUT_CIRC(t -> Math.sqrt(1 - (t - 1) * (t - 1))),
    EASE_INOUT_CIRC(t -> t < 0.5
            ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2
            : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2),

    // BACK
    EASE_IN_BACK(t -> {
        double c = 1.70158;
        return (c + 1) * t * t * t - c * t * t;
    }),
    EASE_OUT_BACK(t -> {
        double c = 1.70158;
        double t1 = t - 1;
        return 1 + (c + 1) * t1 * t1 * t1 + c * t1 * t1;
    }),
    EASE_INOUT_BACK(t -> {
        double c = 1.70158 * 1.525;
        return t < 0.5
                ? (Math.pow(2 * t, 2) * ((c + 1) * 2 * t - c)) / 2
                : (Math.pow(2 * t - 2, 2) * ((c + 1) * (2 * t - 2) + c) + 2) / 2;
    });

    private final DoubleUnaryOperator function;

    Easing(DoubleUnaryOperator function) {
        this.function = function;
    }

    public double ease(double t) {
        return function.applyAsDouble(t);
    }

    public static double easeInPower(double t, double power) {
        return Math.pow(t, power);
    }

    public static double easeOutPower(double t, double power) {
        return 1 - Math.pow(1 - t, power);
    }

    public static double easeInOutPower(double t, double power) {
        return t < 0.5
                ? Math.pow(2 * t, power) / 2
                : 1 - Math.pow(2 - 2 * t, power) / 2;
    }
}