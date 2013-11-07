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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsCacheInfo {
    @XmlAttribute(name = "enabled")
    private boolean enabled = false;
    @XmlElement(name = "elementLifetimeMinutes")
    private int elementLifetimeMinutes = 1440;
    @XmlElement(name = "maxNumItemsInMemory")
    private int maxItemsMemory = 200;
    @XmlElement(name = "enableDiskStore")
    private boolean diskStore = true;
    @XmlElement(name = "maxNumItemsOnDisk")
    private int maxItemsDisk = 2000;

    NcwmsCacheInfo() {
    }

    public NcwmsCacheInfo(boolean enabled, int elementLifetimeMinutes, int maxItemsMemory,
            boolean diskStore, int maxItemsDisk) {
        super();
        this.enabled = enabled;
        this.elementLifetimeMinutes = elementLifetimeMinutes;
        this.maxItemsMemory = maxItemsMemory;
        this.diskStore = diskStore;
        this.maxItemsDisk = maxItemsDisk;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getElementLifetimeMinutes() {
        return elementLifetimeMinutes;
    }

    public int getMaxItemsMemory() {
        return maxItemsMemory;
    }

    public boolean isDiskStore() {
        return diskStore;
    }

    public int getMaxItemsDisk() {
        return maxItemsDisk;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setElementLifetimeMinutes(int elementLifetimeMinutes) {
        this.elementLifetimeMinutes = elementLifetimeMinutes;
    }

    public void setMaxItemsMemory(int maxItemsMemory) {
        this.maxItemsMemory = maxItemsMemory;
    }

    public void setEnableDiskStore(boolean diskStore) {
        this.diskStore = diskStore;
    }

    public void setMaxItemsDisk(int maxItemsDisk) {
        this.maxItemsDisk = maxItemsDisk;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cache");
        sb.append("\n-----");
        sb.append("\nEnabled: ");
        sb.append(enabled);
        sb.append("\nLifetime: ");
        sb.append(elementLifetimeMinutes);
        sb.append("\nDisk store: ");
        sb.append(diskStore);
        sb.append("\nMax items in memory: ");
        sb.append(maxItemsMemory);
        sb.append("\nMax items on disk: ");
        sb.append(maxItemsDisk);
        return sb.toString();
    }
}
