/*
 * Copyright (c) 2009 The University of Reading
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

import java.io.File;
import java.util.Set;
import javax.servlet.ServletContext;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * Top-level configuration object that contains metadata about the server itself
 * and the set of {@link Dataset}s that the server exposes.
 * @author Jon
 */
public interface ServerConfig
{
    /** Returns a human-readable title for this server */
    public String getTitle();

    /**
     * Returns the location of the directory containing the color
     * palette files.
     * @param context The context of the servlet.  Some implementations may
     * place the palette files in a location relative to the servlet.
     */
    public File getPaletteFilesLocation(ServletContext context);

    /** Returns the maximum image that can be requested through GetMap */
    public int getMaxImageWidth();

    /** Returns the maximum height that can be requested through GetMap */
    public int getMaxImageHeight();

    /** Returns a (perhaps-lengthy) description of this server */
    public String getServerAbstract();

    /** Returns a set of keywords that help to describe this server */
    public Set<String> getKeywords();

    /**
     * <p>Returns the date/time at which the data on this server were last updated.
     * This is used for Capabilities document version control in the
     * UPDATESEQUENCE part of the Capabilities document.</p>
     * <p>If the data on this server are constantly being updated, the safest
     * thing to do is to return the current date/time.  This will mean that
     * clients should never cache the Capabilities document.</p>
     * @return the date/time at which the data on this server were last updated.
     */
    public DateTime getLastUpdateTime();

    /**
     * Returns the web address of the organization that is providing this service.
     * @return the web address of the organization that is providing this service.
     */
    public String getServiceProviderUrl();

    public String getContactName();

    public String getContactOrganization();

    public String getContactTelephone();

    public String getContactEmail();

}
