package net.sf.freecol;

import java.awt.Dimension;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.NationOptions.Advantages;

/**
 * This class is responsible for sharing variables between FreeCol.java and CommandLineOptions.java .
 * All variables here are Package Private.
 */
public class Shared
{
    static final Logger logger = Logger.getLogger(FreeCol.class.getName());
    
    /** The FreeCol release version number. */
    static final String FREECOL_VERSION = "0.11.6";
	
    
    // Cli defaults.
	private static final Level  LOGLEVEL_DEFAULT = Level.INFO;
	static final int     TIMEOUT_MIN = 10; // 10s
	
	
    // Cli values.  Often set to null so the default can be applied in
    // the accessor function.
    static boolean checkIntegrity = false,
				   consoleLogging = false,
	               debugStart = false,
	               fastStart = false,
	               headless = false,
	               introVideo = true,
	               javaCheck = true,
	               memoryCheck = true,
	               publicServer = true,
	               sound = true,
	               standAloneServer = false;
    
    
    /** The type of advantages. */
    static Advantages advantages = null;
    
    /** How to name and configure the server. */
    static String serverName = null;
    
    /** The client player name. */
    static String name = null;
    
    /** A stream to get the splash image from. */
    static InputStream splashStream;
    
    /** The level of logging in this game. */
    static Level logLevel = Shared.LOGLEVEL_DEFAULT;
    
    /** A font override. */
    static String fontName = null;
    
    /** How much gui elements get scaled. */
    static float guiScale = FreeCol.GUI_SCALE_DEFAULT;
    
    /** The time out (seconds) for otherwise blocking commands. */
    static int timeout = -1;
    
    /** How to name and configure the server. */
    static int serverPort = -1;
    
    /**
     * The size of window to create, defaults to impossible dimensions
     * to require windowed mode with best determined screen size.
     */
    static Dimension windowSize = new Dimension(-1, -1);
    
    /** The difficulty level id. */
    static String difficulty = null;
    
    /** The TotalConversion / ruleset in play, defaults to "freecol". */
    static String tc = null;
}
