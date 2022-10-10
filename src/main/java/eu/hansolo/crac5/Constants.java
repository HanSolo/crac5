package eu.hansolo.crac5;

import java.io.File;


public class Constants {
    public static final String HOME_FOLDER               = new StringBuilder(System.getProperty("user.home")).append(File.separator).toString();
    public static final String PROPERTIES_FILE_NAME      = "crac5.properties";
    public static final String INTERVAL                  = "interval";
    public static final String INITIAL_CACHE_CLEAN_DELAY = "initial_cache_clean_delay";
    public static final String CACHE_TIMEOUT             = "cache_timeout";
}
