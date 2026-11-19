/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.antibot;

public final class FingerprintingUtil {

    private FingerprintingUtil() {}

    /**
     * Exact Sonar fingerprint generation method:
     * Returns a hex string representing a hash of the username and IP address.
     */
    public static String getFingerprint(final String username, final String hostAddress) {
        if (username == null || hostAddress == null) return "unknown";
        final int hash0 = username.hashCode();
        final int hash1 = hostAddress.hashCode();
        final int combined = hash0 + hash1;
        return Integer.toHexString(hash0 >> 4)
                + Integer.toHexString(combined << 2)
                + Integer.toHexString(hash1 >> 4);
    }
}
