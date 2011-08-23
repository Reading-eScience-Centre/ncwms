/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.ncwms.usagelog.h2;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.h2.tools.Csv;
import org.h2.tools.RunScript;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 * UsageLogger that stores data in an H2 database.  The database is run in 
 * embedded mode, i.e. it is private to the ncWMS application.  Note that the H2
 * database is thread-safe so we make no attempt at thread safety here.
 *
 * @author Jon Blower
 */
public class H2UsageLogger implements UsageLogger
{
    private static final Logger logger = LoggerFactory.getLogger(H2UsageLogger.class);
    
    private static final String INSERT_COMMAND = "INSERT INTO usage_log(request_time, client_ip, " +
            "client_hostname, client_referrer, client_user_agent, http_method, wms_version," +
            "wms_operation, exception_class, exception_message, crs, " + 
            "bbox_minx, bbox_miny, bbox_maxx, bbox_maxy, elevation, time_str, " +
            "num_timesteps, image_width, image_height, layer, dataset_id, " +
            "variable_id, time_to_extract_data_ms, used_cache, feature_info_lon, " +
            "feature_info_lat, feature_info_col, feature_info_row, style_str, " +
            "output_format, transparent, background_color, menu, remote_server_url) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    
    private Connection conn;
    private DataSource dataSource;
    
    // The directory in which we'll store the usage log: will be set by Spring
    private File usageLogDir;
    
    /**
     * Called by Spring to initialize the database
     * @throws Exception if the database could not be initialized
     */
    public void init() throws Exception
    {
        // This will create the directory if it doesn't exist, throwing an
        // Exception if there was an error
        WmsUtils.createDirectory(usageLogDir);
        String databasePath = new File(usageLogDir, "usagelog").getCanonicalPath();
        logger.debug("Usage logger database path = {}", databasePath);
        
        // Load the SQL script file that initializes the database.
        // This script file does nothing if the database is already populated
        InputStream scriptIn =
            Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("/uk/ac/rdg/resc/ncwms/usagelog/h2/init.sql");
        if (scriptIn == null)
        {
            throw new Exception("Can't find initialization script init.sql");
        }
        Reader scriptReader = new InputStreamReader(scriptIn);
        
        try
        {
            // Load the database driver
            Class.forName("org.h2.Driver");
            // Get a connection to the database
            this.conn = DriverManager.getConnection("jdbc:h2:" + databasePath);
            // Set auto-commit to true: can't see any reason why not
            this.conn.setAutoCommit(true);
            
            // Create the DataSource object.  The H2 database only allows a
            // single connection when used in embedded mode, so we use a
            // DataSource that reuses the same Connection object.
            // We set suppressClose = true to ensure that the connection is
            // not closed by the client of the datasource.  We close the connection
            // ourselves in this.close().
            this.dataSource = new SingleConnectionDataSource(this.conn, true);

            // Now run the script to initialize the database
            RunScript.execute(this.conn, scriptReader);
        }
        catch(Exception e)
        {
            // Make sure we clean up before closing
            this.close();
            throw e;
        }
        
        logger.info("H2 Usage Logger initialized");
    }
    
    /**
     * Make an entry in the usage log.  This method does not throw an
     * Exception: all problems with the usage logger must be recorded
     * in the log4j text log.  Implementing methods should make sure they
     * set the time to process the request, by taking System.currentTimeMs()
     * and subtracting logEntry.getRequestTime().
     */
    @Override
    public void logUsage(UsageLogEntry logEntry)
    {
        long startLog = System.currentTimeMillis();
        try
        {
            // Use of setObject allows entries to be null
            PreparedStatement ps = this.conn.prepareStatement(INSERT_COMMAND);
            ps.setObject(1, logEntry.getRequestTime().toDate());
            ps.setObject(2, logEntry.getClientIpAddress());
            ps.setObject(3, logEntry.getClientHost());
            ps.setObject(4, logEntry.getClientReferrer());
            ps.setObject(5, logEntry.getClientUserAgent());
            ps.setObject(6, logEntry.getHttpMethod());
            ps.setObject(7, logEntry.getWmsVersion());
            ps.setObject(8, logEntry.getWmsOperation());
            ps.setString(9, logEntry.getExceptionClass());
            ps.setString(10, logEntry.getExceptionMessage());
            ps.setString(11, logEntry.getCrs());
            ps.setObject(12, logEntry.getBbox() == null ? null : logEntry.getBbox()[0]);
            ps.setObject(13, logEntry.getBbox() == null ? null : logEntry.getBbox()[1]);
            ps.setObject(14, logEntry.getBbox() == null ? null : logEntry.getBbox()[2]);
            ps.setObject(15, logEntry.getBbox() == null ? null : logEntry.getBbox()[3]);
            ps.setString(16, logEntry.getElevation());
            ps.setString(17, logEntry.getTimeString());
            ps.setObject(18, logEntry.getNumTimeSteps());
            ps.setObject(19, logEntry.getWidth());
            ps.setObject(20, logEntry.getHeight());
            ps.setString(21, logEntry.getLayer());
            ps.setString(22, logEntry.getDatasetId());
            ps.setString(23, logEntry.getVariableId());
            ps.setObject(24, logEntry.getTimeToExtractDataMs());
            ps.setBoolean(25, logEntry.isUsedCache());
            ps.setObject(26, logEntry.getFeatureInfoLon());
            ps.setObject(27, logEntry.getFeatureInfoLat());
            ps.setObject(28, logEntry.getFeatureInfoPixelCol());
            ps.setObject(29, logEntry.getFeatureInfoPixelRow());
            ps.setString(30, logEntry.getStyle());
            ps.setString(31, logEntry.getOutputFormat());
            ps.setObject(32, logEntry.getTransparent());
            ps.setString(33, logEntry.getBackgroundColor());
            ps.setString(34, logEntry.getMenu());
            ps.setString(35, logEntry.getRemoteServerUrl());
            ps.executeUpdate();
        }
        catch(SQLException sqle)
        {
            logger.error("Error writing to usage log", sqle);
        }
        finally
        {
            logger.debug("Time to log: {} ms", (System.currentTimeMillis() - 
                startLog));
        }
    }
    
    /**
     * Writes the entire usage log to a CSV file on the given output stream
     */
    public void writeCsv(OutputStream out) throws Exception
    {
        Writer writer = new OutputStreamWriter(out);
        Statement stmt = this.conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * from usage_log");
        Csv.getInstance().write(writer, results);
    }

    /**
     * Returns a DataSource for accessing the database directly.  This method
     * returns the same object with each invocation.
     * @return the DataSource object
     */
    public DataSource getDataSource()
    {
        return this.dataSource;
    }
    
    /**
     * Called by Spring to clean up the database
     */
    public void close()
    {
        if (this.conn != null)
        {
            try { this.conn.close(); }
            catch(SQLException sqle)
            {
                logger.error("Error closing H2 Usage Logger", sqle);
            }
        }
        logger.info("H2 Usage Logger closed");
    }

    /**
     * Will be called by Spring to set the directory for the usage log
     */
    public void setUsageLogDirectory(File usageLogDir)
    {
        this.usageLogDir = usageLogDir;
    }
}
