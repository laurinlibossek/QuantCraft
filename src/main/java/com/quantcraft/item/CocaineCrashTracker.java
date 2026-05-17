package com.quantcraft.item;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CocaineCrashTracker {
    public static final Set<UUID> CRASHING = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private CocaineCrashTracker() {}
}
