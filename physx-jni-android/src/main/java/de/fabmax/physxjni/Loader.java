package de.fabmax.physxjni;

import physx.PlatformChecks;

import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

    public static void load() {
        if (!isLoaded.getAndSet(true)) {
            System.loadLibrary("PhysXJniBindings_64");
            PlatformChecks.setPlatformBit(PlatformChecks.PLATFORM_ANDROID);
        }
    }
}
