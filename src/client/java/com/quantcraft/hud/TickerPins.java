package com.quantcraft.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TickerPins {

    public static final int MAX_PINS = 3;

    private static final List<String> pins = new ArrayList<>(MAX_PINS);

    private TickerPins() {}

    public static boolean pin(String ticker) {
        String t = ticker.toUpperCase();
        if (pins.contains(t))        return false;
        if (pins.size() >= MAX_PINS) return false;
        pins.add(t);
        return true;
    }

    public static boolean unpin(String ticker) {
        return pins.remove(ticker.toUpperCase());
    }

    public static List<String> get() {
        return Collections.unmodifiableList(pins);
    }

    public static boolean isPinned(String ticker) {
        return pins.contains(ticker.toUpperCase());
    }

    public static int count() { return pins.size(); }
}
