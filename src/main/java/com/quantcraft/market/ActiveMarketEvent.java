package com.quantcraft.market;

public class ActiveMarketEvent {
    private final String       headline;
    private final String       affectedTicker;  // null if sector-wide
    private final MarketSector affectedSector;  // null if ticker-specific
    private double             pressurePerTick;
    private int                ticksRemaining;
    private final long         startedAtTick;

    public ActiveMarketEvent(String headline, String affectedTicker, MarketSector affectedSector,
                             double pressurePerTick, int ticksRemaining, long startedAtTick) {
        this.headline        = headline;
        this.affectedTicker  = affectedTicker;
        this.affectedSector  = affectedSector;
        this.pressurePerTick = pressurePerTick;
        this.ticksRemaining  = ticksRemaining;
        this.startedAtTick   = startedAtTick;
    }

    /** Decrements the countdown. Returns false when the event has just expired. */
    public boolean tick() {
        ticksRemaining--;
        return !isExpired();
    }

    public boolean isExpired() { return ticksRemaining <= 0; }

    public String getStatusLine() {
        return headline + " (" + ticksRemaining + " ticks remaining)";
    }

    public void halfPressure() { pressurePerTick *= 0.5; }

    public String       getHeadline()        { return headline; }
    public String       getAffectedTicker()  { return affectedTicker; }
    public MarketSector getAffectedSector()  { return affectedSector; }
    public double       getPressurePerTick() { return pressurePerTick; }
    public int          getTicksRemaining()  { return ticksRemaining; }
    public long         getStartedAtTick()   { return startedAtTick; }
}
