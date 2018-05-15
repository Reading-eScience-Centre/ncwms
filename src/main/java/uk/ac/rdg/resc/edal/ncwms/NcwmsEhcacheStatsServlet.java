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

package uk.ac.rdg.resc.edal.ncwms;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.statistics.StatisticsGateway;
import uk.ac.rdg.resc.edal.cache.EdalCache;

import org.json.JSONObject;

/**
 * An {@link HttpServlet} that returns caching statistics in JSON format.
 *
 * @author Jesse Lopez
 */

public class NcwmsEhcacheStatsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private CacheManager cacheManager;

    public NcwmsEhcacheStatsServlet() throws IOException, Exception {
        super();
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        /*
         * Get the CacheManager
         */
        cacheManager = EdalCache.cacheManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] cacheNames = cacheManager.getCacheNames();

        /*
         * Gather the stats
         */
        JSONObject allStats = new JSONObject();
        for (String name : cacheNames) {
            Cache cache = cacheManager.getCache(name);

            JSONObject cacheStats = new JSONObject();
            StatisticsGateway statisticsGateway = cache.getStatistics();
            cacheStats.put("CacheSize", statisticsGateway.getSize());
            cacheStats.put("HitCount", statisticsGateway.cacheHitCount());
            cacheStats.put("MissCount", statisticsGateway.cacheMissCount());
            cacheStats.put("LocalHeapSize", statisticsGateway.getLocalHeapSize());
            cacheStats.put("LocalHeapHitCount", statisticsGateway.localHeapHitCount());
            cacheStats.put("LocalHeapMissCount", statisticsGateway.localHeapMissCount());
            cacheStats.put("LocalDiskHitCount", statisticsGateway.localDiskHitCount());
            cacheStats.put("LocalDiskMissCount", statisticsGateway.localDiskMissCount());

            allStats.put(name, cacheStats);
        }

        response.setContentType("application/json");
        
        PrintWriter out = response.getWriter();
        out.print(allStats.toString());
        out.flush();
    }
}
