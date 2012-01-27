package uk.ac.rdg.resc.ncwms.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.geotoolkit.factory.Hints;
import org.geotoolkit.referencing.factory.epsg.EpsgInstaller;
import org.opengis.util.FactoryException;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Class which initialises the necessary database for looking up EPSG codes, and
 * sets it as the database to use.
 * 
 * @author Guy Griffiths
 * 
 */
public class EPSGDatabaseInitialiser {
    private DataSource dataSource;
    private Connection conn;
    public EPSGDatabaseInitialiser() {
        try {
            String databasePath = new File("epsgcodes.db").getCanonicalPath();
//            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:" + databasePath);
            conn.setAutoCommit(true);
            dataSource = new SingleConnectionDataSource(conn, true);
            Hints.putSystemDefault(Hints.EPSG_DATA_SOURCE, dataSource);
            EpsgInstaller i = new EpsgInstaller();
            i.setDatabase(conn);
            if(!i.exists()){
                i.call();
//                System.out.println(r.elapsedTime);
                // TODO put logging here (once logging is sorted out)
            }
//            System.out.println("EPSGDI result:");
//            System.out.println(CRS.decode("EPSG:32661"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public DataSource getDataSource(){
        return dataSource;
    }

    /**
     * Called by Spring to clean up the database
     */
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException sqle) {
                // TODO handle this better
            }
        }
    }
}
