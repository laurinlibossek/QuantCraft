package com.quantcraft.market;

public enum MarketSeason {
    RECOVERY   ("Recovery",    "§a", +0.00060,  0.85, 1.2,
            "§aThe economy begins to recover — slow gains ahead. Dividends are strong."),
    EXPANSION  ("Expansion",   "§2", +0.00120,  1.0,  1.0,
            "§2Markets expanding — broad growth across all sectors."),
    PEAK       ("Peak",        "§e", +0.00020,  1.5,  0.7,
            "§eMarket at peak — prices volatile, dividends lean. Consider taking profits."),
    CONTRACTION("Contraction", "§c", -0.00120,  1.2,  0.5,
            "§cContraction underway — prices falling steadily. Shorts and buyers await the bottom.");

    public final String displayName;
    public final String color;
    /** Per-tick pressure coefficient; multiply by stock's basePrice to get absolute nudge. */
    public final double pressureCoeff;
    public final double volatilityMult;
    public final double dividendMult;
    public final String transitionMessage;

    MarketSeason(String displayName, String color, double pressureCoeff,
                 double volatilityMult, double dividendMult, String transitionMessage) {
        this.displayName      = displayName;
        this.color            = color;
        this.pressureCoeff    = pressureCoeff;
        this.volatilityMult   = volatilityMult;
        this.dividendMult     = dividendMult;
        this.transitionMessage = transitionMessage;
    }

    public MarketSeason next() {
        MarketSeason[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
