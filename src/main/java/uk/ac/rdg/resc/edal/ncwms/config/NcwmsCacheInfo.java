/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
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
 ******************************************************************************/

package uk.ac.rdg.resc.edal.ncwms.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import uk.ac.rdg.resc.edal.wms.util.CacheInfo;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsCacheInfo implements CacheInfo {
    @XmlAttribute(name = "enabled")
    private boolean enabled = false;
    @XmlElement(name = "inMemorySizeMB")
    private int inMemorySizeMB = 256;
    @XmlElement(name = "elementLifetimeMinutes")
    private float elementLifetimeMinutes = 0;

    NcwmsCacheInfo() {
    }

    public NcwmsCacheInfo(boolean enabled, int inMemorySizeMB) {
        this.enabled = enabled;
        this.inMemorySizeMB = inMemorySizeMB;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled
     *            Whether this cache should be used or not
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getInMemorySizeMB() {
        return inMemorySizeMB;
    }

    /**
     * @param inMemorySizeMB
     *            The maximum size of this cache in-memory
     */
    public void setInMemorySizeMB(int inMemorySizeMB) {
        this.inMemorySizeMB = inMemorySizeMB;
    }

    /**
     * @param elementLifetimeMinutes
     *            The number of minutes each element should remain in the cache
     *            for. 0 means unlimited.
     */
    public void setElementLifetimeMinutes(float elementLifetimeMinutes) {
        this.elementLifetimeMinutes = elementLifetimeMinutes;
    }

    @Override
    public float getElementLifetimeMinutes() {
        return elementLifetimeMinutes;
    }
}
