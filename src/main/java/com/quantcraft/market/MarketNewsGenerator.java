package com.quantcraft.market;

import java.util.Random;

public class MarketNewsGenerator {
    private static final Random R = new Random();

    public static String generate(String name, double changePct) {
        boolean up  = changePct > 0;
        String  pct = String.format("%.1f%%", Math.abs(changePct));
        String[] uv = {"surges","rallies","climbs","jumps","spikes"};
        String[] dv = {"plunges","tumbles","slides","drops","crashes"};
        String[] uc = {"on strong demand","amid supply shortage","as traders pile in","on positive sentiment"};
        String[] dc = {"on supply glut","amid weak demand","as sellers rush out","following large harvest"};
        return String.format("%s %s %s %s", name,
                up ? uv[R.nextInt(uv.length)] : dv[R.nextInt(dv.length)],
                pct,
                up ? uc[R.nextInt(uc.length)] : dc[R.nextInt(dc.length)]);
    }
}
