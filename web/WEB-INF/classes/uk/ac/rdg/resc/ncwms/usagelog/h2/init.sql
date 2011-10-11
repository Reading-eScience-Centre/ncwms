/* Script to initialize the usage log database.  Will do nothing
   if the database already exists */
/* Note that the H2 database allows for VARCHARs with no specific maximum length */
CREATE TABLE IF NOT EXISTS usage_log
(
    request_time TIMESTAMP NOT NULL, /* The time at which the request was made */
    client_ip VARCHAR NOT NULL, /* The client's IP address */
    client_hostname VARCHAR, /* The host name of the client (if available) */
    client_referrer VARCHAR, /* The site from which the client came (if available) */
    client_user_agent VARCHAR, /* The application/browser that the client is using (if available) */
    http_method VARCHAR NOT NULL, /* The HTTP method that the client is using (will almost always be GET) */
    wms_version VARCHAR, /* The version of WMS that the client requests */
    wms_operation VARCHAR NOT NULL, /* The operation that the client is performing
                                           (GetMap, etc).  Note that this includes non-standard
                                           requests for metadata. */
    exception_class VARCHAR, /* If an exception has been thrown this is the name of the exception class */
    exception_message VARCHAR,
    crs VARCHAR, /* The CRS or SRS code */
    bbox_minx DOUBLE,
    bbox_miny DOUBLE,
    bbox_maxx DOUBLE,
    bbox_maxy DOUBLE,
    elevation VARCHAR,
    time_str VARCHAR,
    num_timesteps SMALLINT, /* The number of timesteps requested */
    image_width SMALLINT,
    image_height SMALLINT,
    layer VARCHAR,
    dataset_id VARCHAR, /* TODO: FK reference to metadata? */
    variable_id VARCHAR, /* TODO: how long should this and dataset_id be? */
    time_to_extract_data_ms INTEGER,
    used_cache BOOLEAN, /* True if we read data from a cache */
    feature_info_lon DOUBLE,
    feature_info_lat DOUBLE,
    feature_info_col SMALLINT,
    feature_info_row SMALLINT,
    style_str VARCHAR,
    output_format VARCHAR,
    transparent BOOLEAN,
    background_color VARCHAR,
    menu VARCHAR,
    remote_server_url VARCHAR /* We use this when a request gets data from a remote server */
);