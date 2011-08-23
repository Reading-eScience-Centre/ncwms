/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.controller;

/**
 * Represents a WMS version number.
 */
class WmsVersion implements Comparable<WmsVersion> {

    private Integer value; // Numerical value of the version number,
    // used for comparisons
    private String str;
    private int hashCode;
    public static final WmsVersion VERSION_1_1_1 = new WmsVersion("1.1.1");
    public static final WmsVersion VERSION_1_3_0 = new WmsVersion("1.3.0");

    /**
     * Creates a new WmsVersion object based on the given String
     * (e.g. "1.3.0")
     * @throws IllegalArgumentException if the given String does not represent
     * a valid WMS version number
     */
    public WmsVersion(String versionStr) {
        String[] els = versionStr.split("\\.");  // regex: split on full stops
        if (els.length != 3) {
            throw new IllegalArgumentException(versionStr +
                    " is not a valid WMS version number");
        }
        int x, y, z;
        try {
            x = Integer.parseInt(els[0]);
            y = Integer.parseInt(els[1]);
            z = Integer.parseInt(els[2]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(versionStr +
                    " is not a valid WMS version number");
        }
        if (y > 99 || z > 99) {
            throw new IllegalArgumentException(versionStr +
                    " is not a valid WMS version number");
        }
        // We can calculate all these values up-front as this object is
        // immutable
        this.str = x + "." + y + "." + z;
        this.value = (100 * 100 * x) + (100 * y) + z;
        this.hashCode = 7 + 79 * this.value.hashCode();
    }

    /**
     * Compares this WmsVersion with the specified Version for order.  Returns a
     * negative integer, zero, or a positive integer as this Version is less
     * than, equal to, or greater than the specified Version.
     */
    @Override
    public int compareTo(WmsVersion otherVersion) {
        return this.value.compareTo(otherVersion.value);
    }

    /**
     * @return String representation of this version, e.g. "1.3.0"
     */
    @Override
    public String toString() {
        return this.str;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof WmsVersion) {
            final WmsVersion other = (WmsVersion) obj;
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
