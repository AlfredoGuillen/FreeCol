package net.sf.freecol;

import static net.sf.freecol.common.util.CollectionUtils.find;
import static net.sf.freecol.common.util.CollectionUtils.map;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.NationOptions.Advantages;

/**
 * This class is responsible for handling the command-line arguments and starting the client-GUI.
 */
public class CommandLineOptions
{ 
    /** The difficulty levels. */
    public static final String[] DIFFICULTIES =
    {
        "veryEasy", "easy", "medium", "hard", "veryHard"
    };
	
    // Cli defaults.
    public static final int GUI_SCALE_MAX_PCT = 200;
    public static final int GUI_SCALE_MIN_PCT = 100;
    public static final int GUI_SCALE_STEP_PCT = 25;
    public static final float GUI_SCALE_MIN = GUI_SCALE_MIN_PCT / 100.0f;
    public static final float GUI_SCALE_MAX = GUI_SCALE_MAX_PCT / 100.0f;
    public static final float GUI_SCALE_STEP = GUI_SCALE_STEP_PCT / 100.0f;
    private static final int    EUROPEANS_MIN = 1;
	
	public CommandLineOptions()
	{
		
	}
	
    /**
     * Prints the usage message and exits.
     *
     * @param options The command line <code>Options</code>.
     * @param status The status to exit with.
     */
    private static void printUsage(Options options, int status)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -Xmx 256M -jar freecol.jar [OPTIONS]",
                            options);
        System.exit(status);
    }
    
    /**
     * Just gripe to System.err.
     *
     * @param template A <code>StringTemplate</code> to print.
     */
    public static void gripe(StringTemplate template)
    {
        System.err.println(Messages.message(template));
    }
	
    // Accessors, mutators and support for the cli variables.
	
    /**
     * Gets a comma separated list of localized advantage type names.
     *
     * @return A list of advantage types.
     */
    private static String getValidAdvantages()
    {
        return Arrays.stream(Advantages.values())
            .map(a -> Messages.getName(a)).collect(Collectors.joining(","));
    }
    
    /**
     * Gets the valid scale factors for the GUI.
     * 
     * @return A string containing these.
     */
    public static String getValidGUIScales()
    {
        String result = "";
        for(int i=GUI_SCALE_MIN_PCT; i<GUI_SCALE_MAX_PCT; i+=GUI_SCALE_STEP_PCT)
        {
            result += i + ",";
        }
        result += GUI_SCALE_MAX_PCT;
        return result;
    }
    
    /**
     * Sets the advantages type.
     *
     * Called from NewPanel when a selection is made.
     *
     * @param advantages The name of the new advantages type.
     * @return The type of advantages set, or null if none.
     */
    private static Advantages selectAdvantages(String advantages)
    {
        Advantages adv = find(Advantages.values(),
            a -> Messages.getName(a).equals(advantages), null);
        if (adv != null) FreeCol.setAdvantages(adv);
        return adv;
    }
    
    /**
     * Sets the log level.
     *
     * @param arg The log level to set.
     */
    private static void setLogLevel(String arg)
    {
        Shared.logLevel = Level.parse(arg.toUpperCase());
    }
    
    /**
     * Sets the window size.
     *
     * Does not fail because any empty or invalid value is interpreted as
     * `windowed but use as much screen as possible'.
     *
     * @param arg The window size specification.
     */
    private static void setWindowSize(String arg)
    {
        String[] xy;
        if (arg != null
            && (xy = arg.split("[^0-9]")) != null
            && xy.length == 2)
        {
            try
            {
                Shared.windowSize = new Dimension(Integer.parseInt(xy[0]),
                                           Integer.parseInt(xy[1]));
            } catch (NumberFormatException nfe) {}
        }
        if (Shared.windowSize == null) Shared.windowSize = new Dimension(-1, -1);
    }
    
    /**
     * Selects a difficulty level.
     *
     * @param arg The supplied difficulty argument.
     * @return The name of the selected difficulty, or null if none.
     */
    public static String selectDifficulty(String arg)
    {
        String difficulty = find(map(DIFFICULTIES, d -> "model.difficulty."+d),
            k -> Messages.getName(k).equals(arg), null);
        if (difficulty != null) FreeCol.setDifficulty(difficulty);
        return difficulty;
    }
    
    /**
     * Sets the scale for GUI elements.
     * 
     * @param arg The optional command line argument to be parsed.
     * @return If the argument was correctly formatted.
     */
    public static boolean setGUIScale(String arg)
    {
        boolean valid = true;
        if(arg == null)
        {
            Shared.guiScale = GUI_SCALE_MAX;
        }
        
        else
        {
            try
            {
                int n = Integer.parseInt(arg);
                if (n < GUI_SCALE_MIN_PCT)
                {
                    valid = false;
                    n = GUI_SCALE_MIN_PCT;
                }
                
                else if(n > GUI_SCALE_MAX_PCT)
                {
                    valid = false;
                    n = GUI_SCALE_MAX_PCT;
                }
                
                else if(n % GUI_SCALE_STEP_PCT != 0)
                {
                    valid = false;
                }
                Shared.guiScale = ((float)(n / GUI_SCALE_STEP_PCT)) * GUI_SCALE_STEP;
            }
            
            catch (NumberFormatException nfe)
            {
                valid = false;
                Shared.guiScale = GUI_SCALE_MAX;
            }
        }
        return valid;
    }
    
    /**
     * Gets the names of the valid difficulty levels.
     *
     * @return The valid difficulty levels, comma separated.
     */
    public static String getValidDifficulties()
    {
        return Arrays.stream(DIFFICULTIES)
            .map(d -> Messages.getName("model.difficulty." + d))
            .collect(Collectors.joining(","));
    }
    
    /**
     * Selects a European nation count.
     *
     * @param arg The supplied count argument.
     * @return A valid nation number, or negative on error.
     */
    public static int selectEuropeanCount(String arg)
    {
        try
        {
            int n = Integer.parseInt(arg);
            if (n >= EUROPEANS_MIN)
            {
                FreeCol.setEuropeanCount(n);
                return n;
            }
        } catch (NumberFormatException nfe) {}
        return -1;
    }
    
    /**
     * Sets the timeout.
     *
     * @param timeout A string containing the new timeout.
     * @return True if the timeout was set.
     */
    public static boolean setTimeout(String timeout)
    {
        try
        {
            int result = Integer.parseInt(timeout);
            if (result >= Shared.TIMEOUT_MIN)
            {
                Shared.timeout = result;
                return true;
            }
        } catch (NumberFormatException nfe) {}
        return false;
    }
    
    /**
     * Sets the server port.
     *
     * @param arg The server port number.
     * @return True if the port was set.
     */
    public static boolean setServerPort(String arg)
    {
        if (arg == null) return false;
        try
        {
            Shared.serverPort = Integer.parseInt(arg);
        } catch (NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
	
    //Command Line
    
    /**
     * Builds command line options with their arguments.
     *
     * @param args Options object.
     */
    private static void CreateOptions(Options options)
    {
        final String help = Messages.message("cli.help");
        final File dummy = new File("dummy");
        final String argDirectory = Messages.message("cli.arg.directory");
    	
    	//Usage options.
        OptionBuilder.withLongOpt("usage");
		OptionBuilder.withDescription(help);
        options.addOption(OptionBuilder.create());
		
		// Help options.
        OptionBuilder.withLongOpt("help");
		OptionBuilder.withDescription(help);
		options.addOption(OptionBuilder.create());

        OptionBuilder.withLongOpt("freecol-data");
		OptionBuilder.withDescription(Messages.message("cli.freecol-data"));
		OptionBuilder.withArgName(argDirectory);
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
		// Special options handled early.
        OptionBuilder.withLongOpt("default-locale");
		OptionBuilder.withDescription(Messages.message("cli.default-locale"));
		OptionBuilder.withArgName(Messages.message("cli.arg.locale"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());

        OptionBuilder.withLongOpt("advantages");
		OptionBuilder.withDescription
		(
				Messages.message
				(
						StringTemplate.template("cli.advantages")
                                  	  .addName("%advantages%", getValidAdvantages())
                )
		);
		OptionBuilder.withArgName(Messages.message("cli.arg.advantages"));
		OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());
		
		// Ordinary options, handled here.
        OptionBuilder.withLongOpt("check-savegame");
		OptionBuilder.withDescription(Messages.message("cli.check-savegame"));
		OptionBuilder.withArgName(Messages.message("cli.arg.file"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("clientOptions");
		OptionBuilder.withDescription(Messages.message("cli.clientOptions"));
		OptionBuilder.withArgName(Messages.message("cli.arg.clientOptions"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("debug");
		OptionBuilder.withDescription(Messages.message(StringTemplate
                     .template("cli.debug")
                     .addName("%modes%", FreeColDebugger.getDebugModes())));
		OptionBuilder.withArgName(Messages.message("cli.arg.debug"));
		OptionBuilder.hasOptionalArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("debug-run");
		OptionBuilder.withDescription(Messages.message("cli.debug-run"));
		OptionBuilder.withArgName(Messages.message("cli.arg.debugRun"));
		OptionBuilder.hasOptionalArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("debug-start");
		OptionBuilder.withDescription(Messages.message("cli.debug-start"));
		options.addOption(OptionBuilder.create());
        OptionBuilder.withLongOpt("difficulty");
		OptionBuilder.withDescription(Messages.message("cli.difficulty"));
		OptionBuilder.withArgName(Messages.message("cli.arg.difficulty"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("europeans");
		OptionBuilder.withDescription(Messages.message("cli.european-count"));
		OptionBuilder.withArgName(Messages.message("cli.arg.europeans"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("fast");
		OptionBuilder.withDescription(Messages.message("cli.fast"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("font");
		OptionBuilder.withDescription(Messages.message("cli.font"));
		OptionBuilder.withArgName(Messages.message("cli.arg.font"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("full-screen");
		OptionBuilder.withDescription(Messages.message("cli.full-screen"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("gui-scale");
		OptionBuilder.withDescription(Messages.message(StringTemplate
                     .template("cli.gui-scale")
                     .addName("%scales%", getValidGUIScales())));
		OptionBuilder.withArgName(Messages.message("cli.arg.gui-scale"));
		OptionBuilder.hasOptionalArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("headless");
		OptionBuilder.withDescription(Messages.message("cli.headless"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("load-savegame");
		OptionBuilder.withDescription(Messages.message("cli.load-savegame"));
		OptionBuilder.withArgName(Messages.message("cli.arg.file"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("log-console");
		OptionBuilder.withDescription(Messages.message("cli.log-console"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("log-file");
		OptionBuilder.withDescription(Messages.message("cli.log-file"));
		OptionBuilder.withArgName(Messages.message("cli.arg.name"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("log-level");
		OptionBuilder.withDescription(Messages.message("cli.log-level"));
		OptionBuilder.withArgName(Messages.message("cli.arg.loglevel"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("name");
		OptionBuilder.withDescription(Messages.message("cli.name"));
		OptionBuilder.withArgName(Messages.message("cli.arg.name"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("no-intro");
		OptionBuilder.withDescription(Messages.message("cli.no-intro"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("no-java-check");
		OptionBuilder.withDescription(Messages.message("cli.no-java-check"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("no-memory-check");
		OptionBuilder.withDescription(Messages.message("cli.no-memory-check"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("no-sound");
		OptionBuilder.withDescription(Messages.message("cli.no-sound"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("no-splash");
		OptionBuilder.withDescription(Messages.message("cli.no-splash"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("private");
		OptionBuilder.withDescription(Messages.message("cli.private"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("seed");
		OptionBuilder.withDescription(Messages.message("cli.seed"));
		OptionBuilder.withArgName(Messages.message("cli.arg.seed"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("server");
		OptionBuilder.withDescription(Messages.message("cli.server"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("server-name");
		OptionBuilder.withDescription(Messages.message("cli.server-name"));
		OptionBuilder.withArgName(Messages.message("cli.arg.name"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("server-port");
		OptionBuilder.withDescription(Messages.message("cli.server-port"));
		OptionBuilder.withArgName(Messages.message("cli.arg.port"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("splash");
		OptionBuilder.withDescription(Messages.message("cli.splash"));
		OptionBuilder.withArgName(Messages.message("cli.arg.file"));
		OptionBuilder.hasOptionalArg();
		options.addOption(OptionBuilder.create());

        OptionBuilder.withLongOpt("tc");
		OptionBuilder.withDescription(Messages.message("cli.tc"));
		OptionBuilder.withArgName(Messages.message("cli.arg.name"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("timeout");
		OptionBuilder.withDescription(Messages.message("cli.timeout"));
		OptionBuilder.withArgName(Messages.message("cli.arg.timeout"));
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("user-cache-directory");
		OptionBuilder.withDescription(Messages.message("cli.user-cache-directory"));
		OptionBuilder.withArgName(argDirectory);
		OptionBuilder.withType(dummy);
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("user-config-directory");
		OptionBuilder.withDescription(Messages.message("cli.user-config-directory"));
		OptionBuilder.withArgName(argDirectory);
		OptionBuilder.withType(dummy);
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("user-data-directory");
		OptionBuilder.withDescription(Messages.message("cli.user-data-directory"));
		OptionBuilder.withArgName(argDirectory);
		OptionBuilder.withType(dummy);
		OptionBuilder.hasArg();
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("version");
		OptionBuilder.withDescription(Messages.message("cli.version"));
		options.addOption(OptionBuilder.create());
		
        OptionBuilder.withLongOpt("windowed");
		OptionBuilder.withDescription(Messages.message("cli.windowed"));
		OptionBuilder.withArgName(Messages.message("cli.arg.dimensions"));
		OptionBuilder.hasOptionalArg();
		options.addOption(OptionBuilder.create());
    }
	
    /**
     * Processes the command-line arguments and takes appropriate
     * actions for each of them.
     *
     * @param args The command-line arguments.
     */
    static void handleArgs(String[] args)
    {
        Options options = new Options();

        CreateOptions(options);

        CommandLineParser parser = new PosixParser();
        boolean usageError = false;
        
        try
        {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help") || line.hasOption("usage"))
            {
                printUsage(options, 0);
            }

            /*
             * "default-locale" already handled in main().
             * "freecol-data" already handled in main().
            */

            if (line.hasOption("advantages"))
            {
                String arg = line.getOptionValue("advantages");
                Advantages a = selectAdvantages(arg);
                if (a == null)
                {
                    FreeCol.fatal(StringTemplate.template("cli.error.advantages")
                        .addName("%advantages%", getValidAdvantages())
                        .addName("%arg%", arg));
                }
            }

            if (line.hasOption("check-savegame"))
            {
                String arg = line.getOptionValue("check-savegame");
                if (!FreeColDirectories.setSavegameFile(arg))
                {
                	FreeCol.fatal(StringTemplate.template("cli.err.save")
                        .addName("%string%", arg));
                }
                Shared.checkIntegrity = true;
                Shared.standAloneServer = true;
            }

            if (line.hasOption("clientOptions"))
            {
                String fileName = line.getOptionValue("clientOptions");
                if (!FreeColDirectories.setClientOptionsFile(fileName))
                {
                    // Not fatal.
                    gripe(StringTemplate.template("cli.error.clientOptions")
                        .addName("%string%", fileName));
                }
            }

            if (line.hasOption("debug")) {
                // If the optional argument is supplied use limited mode.
                String arg = line.getOptionValue("debug");
                if (arg == null || arg.isEmpty()) {
                    // Let empty argument default to menus functionality.
                    arg = FreeColDebugger.DebugMode.MENUS.toString();
                }
                if (!FreeColDebugger.setDebugModes(arg)) { // Not fatal.
                    gripe(StringTemplate.template("cli.error.debug")
                        .addName("%modes%", FreeColDebugger.getDebugModes()));
                }
                // user set log level has precedence
                if (!line.hasOption("log-level")) Shared.logLevel = Level.FINEST;
            }
            if (line.hasOption("debug-run")) {
                FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
                FreeColDebugger.configureDebugRun(line.getOptionValue("debug-run"));
            }
            if (line.hasOption("debug-start")) {
                Shared.debugStart = true;
                FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
            }

            if (line.hasOption("difficulty")) {
                String arg = line.getOptionValue("difficulty");
                String difficulty = selectDifficulty(arg);
                if (difficulty == null) {
                	FreeCol.fatal(StringTemplate.template("cli.error.difficulties")
                        .addName("%difficulties%", getValidDifficulties())
                        .addName("%arg%", arg));
                }
            }

            if (line.hasOption("europeans")) {
                int e = selectEuropeanCount(line.getOptionValue("europeans"));
                if (e < 0) {
                    gripe(StringTemplate.template("cli.error.europeans")
                        .addAmount("%min%", EUROPEANS_MIN));
                }
            }

            if (line.hasOption("fast")) {
                Shared.fastStart = true;
                Shared.introVideo = false;
            }

            if (line.hasOption("font")) {
                Shared.fontName = line.getOptionValue("font");
            }

            if (line.hasOption("full-screen")) {
                Shared.windowSize = null;
            }

            if (line.hasOption("gui-scale")) {
                String arg = line.getOptionValue("gui-scale");
                if(!setGUIScale(arg)) {
                    gripe(StringTemplate.template("cli.error.gui-scale")
                        .addName("%scales%", getValidGUIScales())
                        .addName("%arg%", arg));
                }
            }

            if (line.hasOption("headless")) {
                Shared.headless = true;
            }

            if (line.hasOption("load-savegame")) {
                String arg = line.getOptionValue("load-savegame");
                if (!FreeColDirectories.setSavegameFile(arg)) {
                	FreeCol.fatal(StringTemplate.template("cli.error.save")
                        .addName("%string%", arg));
                }
            }

            if (line.hasOption("log-console")) {
                Shared.consoleLogging = true;
            }
            if (line.hasOption("log-file")) {
                FreeColDirectories.setLogFilePath(line.getOptionValue("log-file"));
            }
            if (line.hasOption("log-level")) {
                setLogLevel(line.getOptionValue("log-level"));
            }

            if (line.hasOption("name")) {
                FreeCol.setName(line.getOptionValue("name"));
            }

            if (line.hasOption("no-intro")) {
                Shared.introVideo = false;
            }
            if (line.hasOption("no-java-check")) {
            	Shared.javaCheck = false;
            }
            if (line.hasOption("no-memory-check")) {
            	Shared.memoryCheck = false;
            }
            if (line.hasOption("no-sound")) {
            	Shared.sound = false;
            }
            if (line.hasOption("no-splash")) {
                Shared.splashStream = null;
            }

            if (line.hasOption("private")) {
            	Shared.publicServer = false;
            }

            if (line.hasOption("server")) {
            	Shared.standAloneServer = true;
            }
            if (line.hasOption("server-name")) {
                Shared.serverName = line.getOptionValue("server-name");
            }
            if (line.hasOption("server-port")) {
                String arg = line.getOptionValue("server-port");
                if (!setServerPort(arg)) {
                	FreeCol.fatal(StringTemplate.template("cli.error.serverPort")
                        .addName("%string%", arg));
                }
            }

            if (line.hasOption("seed")) {
                FreeColSeed.setFreeColSeed(line.getOptionValue("seed"));
            }

            if (line.hasOption("splash")) {
                String splash = line.getOptionValue("splash");
                try {
                    FileInputStream fis = new FileInputStream(splash);
                    Shared.splashStream = fis;
                } catch (FileNotFoundException fnfe) {
                    gripe(StringTemplate.template("cli.error.splash")
                        .addName("%name%", splash));
                }
            }

            if (line.hasOption("tc")) {
                FreeCol.setTC(line.getOptionValue("tc")); // Failure is deferred.
            }

            if (line.hasOption("timeout")) {
                String arg = line.getOptionValue("timeout");
                if (!setTimeout(arg)) { // Not fatal
                    gripe(StringTemplate.template("cli.error.timeout")
                        .addName("%string%", arg)
                        .addName("%minimum%", Integer.toString(Shared.TIMEOUT_MIN)));
                }
            }

            userOptions(line);
            
            if (line.hasOption("version")) {
                System.out.println("FreeCol " + FreeCol.getVersion());
                System.exit(0);
            }

            if (line.hasOption("windowed")) {
                String arg = line.getOptionValue("windowed");
                setWindowSize(arg); // Does not fail
            }

        } catch (ParseException e) {
            System.err.println("\n" + e.getMessage() + "\n");
            usageError = true;
        }
        if (usageError) printUsage(options, 1);
    }
    
    private static void userOptions(CommandLine line)
    {
        if (line.hasOption("user-cache-directory"))
        {
            String arg = line.getOptionValue("user-cache-directory");
            String errMsg = FreeColDirectories.setUserCacheDirectory(arg);
            if (errMsg != null) // Not fatal.
            {
                gripe(StringTemplate.template(errMsg)
                    .addName("%string%", arg));
            }
        }

        if (line.hasOption("user-config-directory"))
        {
            String arg = line.getOptionValue("user-config-directory");
            String errMsg = FreeColDirectories.setUserConfigDirectory(arg);
            if (errMsg != null) // Not fatal.
            {
                gripe(StringTemplate.template(errMsg)
                    .addName("%string%", arg));
            }
        }

        if (line.hasOption("user-data-directory"))
        {
            String arg = line.getOptionValue("user-data-directory");
            String errMsg = FreeColDirectories.setUserDataDirectory(arg);
            if (errMsg != null) // fatal, unable to save.
            {
            	FreeCol.fatal(StringTemplate.template(errMsg)
                    .addName("%string%", arg));
            }
        }
    }
}
