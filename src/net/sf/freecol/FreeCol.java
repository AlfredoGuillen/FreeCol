/**
 *  Copyright (C) 2002-2015   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.logging.DefaultHandler;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;


/**
 * This class is responsible for starting the stand-alone server.
 *
 * @see net.sf.freecol.client.FreeColClient FreeColClient
 * @see net.sf.freecol.server.FreeColServer FreeColServer
 */
public final class FreeCol
{

    /** The extension for FreeCol saved games. */
    public static final String  FREECOL_SAVE_EXTENSION = "fsg";

    /** The Java version. */
    private static final String JAVA_VERSION
        = System.getProperty("java.version");

    /** The maximum available memory. */
    private static final long MEMORY_MAX = Runtime.getRuntime().maxMemory();

    public static final String  CLIENT_THREAD = "FreeColClient:";
    public static final String  SERVER_THREAD = "FreeColServer:";
    public static final String  METASERVER_THREAD = "FreeColMetaServer:";

    public static final String  META_SERVER_ADDRESS = "meta.freecol.org";
    public static final int     META_SERVER_PORT = 3540;

    /** Specific revision number (currently the git tag of trunk at release) */
    private static String       freeColRevision = null;

    /** The locale, either default or command-line specified. */
    private static Locale       locale = null;


    // Cli defaults.
    private static final Advantages ADVANTAGES_DEFAULT = Advantages.SELECTABLE;
    private static final String DIFFICULTY_DEFAULT = "model.difficulty.medium";
    private static final int    EUROPEANS_DEFAULT = 4;
    public static final float GUI_SCALE_DEFAULT = 1.0f;
    private static final String JAVA_VERSION_MIN = "1.8";
    private static final int    MEMORY_MIN = 128; // Mbytes
    private static final int    PORT_DEFAULT = 3541;
    private static final String SPLASH_DEFAULT = "splash.jpg";
    private static final String TC_DEFAULT = "freecol";
    public static final int     TIMEOUT_DEFAULT = 60; // 1 minute
    
    // Cli values.  Often set to null so the default can be applied in
    // the accessor function.
    
    /** The number of European nations to enable by default. */
    private static int europeanCount = EUROPEANS_DEFAULT;
   
    private FreeCol() {} // Hide constructor

    /**
     * The entrypoint.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        freeColRevision = Shared.FREECOL_VERSION;
        JarURLConnection juc;
        try {
            juc = getJarURLConnection(FreeCol.class);
        } catch (IOException ioe) {
            juc = null;
            System.err.println("Unable to open class jar: "
                + ioe.getMessage());
        }
        if (juc != null) {
            try {
                String revision = readVersion(juc);
                if (revision != null) {
                    freeColRevision += " (Revision: " + revision + ")";
                }
            } catch (Exception e) {
                System.err.println("Unable to load Manifest: "
                    + e.getMessage());
            }
            try {
                Shared.splashStream = getDefaultSplashStream(juc);
            } catch (Exception e) {
                System.err.println("Unable to open default splash: "
                    + e.getMessage());
            }
        }

        // Java bug #7075600 causes BR#2554.  The workaround is to set
        // the following property.  Remove if/when they fix Java.
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        // We can not even emit localized error messages until we find
        // the data directory, which might have been specified on the
        // command line.
        String dataDirectoryArg = findArg("--freecol-data", args);
        String err = FreeColDirectories.setDataDirectory(dataDirectoryArg);
        if (err != null) fatal(err); // This must not fail.

        // Now we have the data directory, establish the base locale.
        // Beware, the locale may change!
        String localeArg = findArg("--default-locale", args);
        if (localeArg == null) {
            locale = Locale.getDefault();
        } else {
            int index = localeArg.indexOf('.'); // Strip encoding if present
            if (index > 0) localeArg = localeArg.substring(0, index);
            locale = Messages.getLocale(localeArg);
        }
        Messages.loadMessageBundle(locale);

        // Now that we can emit error messages, parse the other
        // command line arguments.
        CommandLineOptions.handleArgs(args);

        // Do the potentially fatal system checks as early as possible.
        if (Shared.javaCheck && JAVA_VERSION_MIN.compareTo(JAVA_VERSION) > 0) {
            fatal(StringTemplate.template("main.javaVersion")
                .addName("%version%", JAVA_VERSION)
                .addName("%minVersion%", JAVA_VERSION_MIN));
        }
        if (Shared.memoryCheck && MEMORY_MAX < MEMORY_MIN * 1000000) {
            fatal(StringTemplate.template("main.memory")
                .addAmount("%memory%", MEMORY_MAX)
                .addAmount("%minMemory%", MEMORY_MIN));
        }

        // Having parsed the command line args, we know where the user
        // directories should be, so we can set up the rest of the
        // file/directory structure.
        String userMsg = FreeColDirectories.setUserDirectories();

        // Now we have the log file path, start logging.
        final Logger baseLogger = Logger.getLogger("");
        final Handler[] handlers = baseLogger.getHandlers();
        for (Handler handler : handlers) {
            baseLogger.removeHandler(handler);
        }
        String logFile = FreeColDirectories.getLogFilePath();
        try {
            baseLogger.addHandler(new DefaultHandler(Shared.consoleLogging, logFile));
            Logger freecolLogger = Logger.getLogger("net.sf.freecol");
            freecolLogger.setLevel(Shared.logLevel);
        } catch (FreeColException e) {
            System.err.println("Logging initialization failure: "
                + e.getMessage());
            e.printStackTrace();
        }
        Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable e) -> {
                baseLogger.log(Level.WARNING, "Uncaught exception from thread: " + thread, e);
            });

        // Now we can find the client options, allow the options
        // setting to override the locale, if no command line option
        // had been specified.
        // We have users whose machines default to Finnish but play
        // FreeCol in English.
        // If the user has selected automatic language selection, do
        // nothing, since we have already set up the default locale.
        if (localeArg == null) {
            String clientLanguage = ClientOptions.getLanguageOption();
            Locale clientLocale;
            if (clientLanguage != null
                && !Messages.AUTOMATIC.equalsIgnoreCase(clientLanguage)
                && (clientLocale = Messages.getLocale(clientLanguage)) != locale) {
                locale = clientLocale;
                Messages.loadMessageBundle(locale);
                Shared.logger.info("Loaded messages for " + locale);
            }
        }

        // Now we have the user mods directory and the locale is now
        // stable, load the mods and their messages.
        Mods.loadMods();
        Messages.loadModMessageBundle(locale);

        // Report on where we are.
        if (userMsg != null) Shared.logger.info(Messages.message(userMsg));
        Shared.logger.info(getConfiguration().toString());

        // Ready to specialize into client or server.
        if (Shared.standAloneServer)
        {
            startServer();
        } else {
            startClient(userMsg);
        }
    }

    /**
     * Exit printing fatal error message.
     *
     * @param template A <code>StringTemplate</code> to print.
     */
    public static void fatal(StringTemplate template)
    {
        fatal(Messages.message(template));
    }
    
    /**
     * Exit printing fatal error message.
     *
     * @param err The error message to print.
     */
    public static void fatal(String err)
    {
        if (err == null || err.isEmpty())
        {
            err = "Bogus null fatal error message";
            Thread.dumpStack();
        }
        System.err.println(err);
        System.exit(1);
    }

    /**
     * Get the JarURLConnection from a class.
     *
     * @return The <code>JarURLConnection</code>.
     */
    private static JarURLConnection getJarURLConnection(Class<FreeCol> c) throws IOException {
        String resourceName = "/" + c.getName().replace('.', '/') + ".class";
        URL url = c.getResource(resourceName);
        return (JarURLConnection)url.openConnection();
    }
        
    /**
     * Extract the package version from the class.
     *
     * @param juc The <code>JarURLConnection</code> to extract from.
     * @return A value of the package version attribute.
     */
    private static String readVersion(JarURLConnection juc) throws IOException {
        Manifest mf = juc.getManifest();
        return (mf == null) ? null
            : mf.getMainAttributes().getValue("Package-Version");
    }

    /**
     * Get a stream for the default splash file.
     *
     * Note: Not bothering to check for nulls as this is called in try
     * block that ignores all exceptions.
     *
     * @param juc The <code>JarURLConnection</code> to extract from.
     * @return A suitable <code>InputStream</code>, or null on error.
     */
    private static InputStream getDefaultSplashStream(JarURLConnection juc) throws IOException {
        JarFile jf = juc.getJarFile();
        ZipEntry ze = jf.getEntry(SPLASH_DEFAULT);
        return jf.getInputStream(ze);
    }

    /**
     * Just gripe to System.err.
     *
     * @param key A message key.
     */
    public static void gripe(String key) {
        System.err.println(Messages.message(key));
    }

    /**
     * Find an option before the real option handling can get started.
     * Takes care to use the *last* instance.
     *
     * @param option The option to find.
     * @param args The  command-line arguments.
     * @return The option's parameter.
     */
    private static String findArg(String option, String[] args) {
        for (int i = args.length - 2; i >= 0; i--) {
            if (option.equals(args[i])) {
                return args[i+1];
            }
        }
        return null;
    }

    /**
     * Get the specification from a given TC file.
     *
     * @param tcf The <code>FreeColTcFile</code> to load.
     * @param advantages An optional <code>Advantages</code> setting.
     * @param difficulty An optional difficulty level.
     * @return A <code>Specification</code>.
     */
    public static Specification loadSpecification(FreeColTcFile tcf,
                                                  Advantages advantages,
                                                  String difficulty) {
        Specification spec = null;
        try {
            if (tcf != null) spec = tcf.getSpecification();
        } catch (IOException ioe) {
            System.err.println("Spec read failed in " + tcf.getId()
                + ": " + ioe.getMessage() + "\n");
        }
        if (spec != null) spec.prepare(advantages, difficulty);
        return spec;
    }

    /**
     * Get the specification from the specified TC.
     *
     * @return A <code>Specification</code>, quits on error.
     */
    private static Specification getTCSpecification() {
        Specification spec = loadSpecification(getTCFile(), getAdvantages(),
                                               getDifficulty());
        if (spec == null) {
            fatal(StringTemplate.template("cli.error.badTC")
                .addName("%tc%", getTC()));
        }
        return spec;
    }

    // Accessors, mutators and support for the cli variables.

    /**
     * Sets the difficulty level.
     *
     * @param difficulty The new difficulty.
     */
    public static void setDifficulty(String difficulty)
    {
        Shared.difficulty = difficulty;
    }
    
    /**
     * Sets the difficulty level.
     *
     * @param difficulty The actual <code>OptionGroup</code>
     *     containing the difficulty level.
     */
    public static void setDifficulty(OptionGroup difficulty)
    {
        setDifficulty(difficulty.getId());
    }
    
    /**
     * Gets the default advantages type.
     *
     * @return Usually Advantages.SELECTABLE, but can be overridden at the
     *     command line.
     */
    public static Advantages getAdvantages()
    {
        return (Shared.advantages == null) ? ADVANTAGES_DEFAULT
            : Shared.advantages;
    }

    /**
     * Gets the difficulty level.
     *
     * @return The name of a difficulty level.
     */
    public static String getDifficulty() {
        return (Shared.difficulty == null) ? DIFFICULTY_DEFAULT : Shared.difficulty;
    }

    /**
     * Gets the user name.
     *
     * @return The user name, defaults to the user.name property, then to
     *     the "main.defaultPlayerName" message value.
     */
    public static String getName()
    {
        return (Shared.name != null) ? Shared.name
            : System.getProperty("user.name",
                                 Messages.message("main.defaultPlayerName"));
    }

    /**
     * Get the selected locale.
     *
     * @return The <code>Locale</code> currently in use.
     */
    public static Locale getLocale() {
        return FreeCol.locale;
    }
    
    /**
     * Gets the current revision of game.
     *
     * @return The current version and SVN Revision of the game.
     */
    public static String getRevision() {
        return freeColRevision;
    }
    
    /**
     * Sets the advantages type.
     *
     * @param advantages The new <code>Advantages</code> type.
     */
    public static void setAdvantages(Advantages advantages)
    {
        Shared.advantages = advantages;
    }
    
    /**
     * Sets the Total-Conversion.
     *
     * Called from NewPanel when a selection is made.
     *
     * @param tc The name of the new total conversion.
     */
    public static void setTC(String tc)
    {
        Shared.tc = tc;
    }

    /**
     * Get the default server host name.
     *
     * @return The host name.
     */
    public static String getServerHost() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }
    
    /**
     * Get the number of European nations to enable by default.
     */
    public static int getEuropeanCount()
    {
        return europeanCount;
    }
    
    /**
     * Sets the number of enabled European nations.
     *
     * @param n The number of nations to enable.
     */
    public static void setEuropeanCount(int n)
    {
        europeanCount = n;
    }
    
    /**
     * Sets the user name.
     *
     * @param name The new user name.
     */
    public static void setName(String name)
    {
        Shared.name = name;
        Shared.logger.info("Set FreeCol.name = " + name);
    }

    /**
     * Gets the server network port.
     *
     * @return The port number.
     */
    public static int getServerPort() 
    {
        return (Shared.serverPort < 0) ? PORT_DEFAULT : Shared.serverPort;
    }

    /**
     * Gets the current Total-Conversion.
     *
     * @return Usually TC_DEFAULT, but can be overridden at the command line.
     */
    public static String getTC() {
        return (Shared.tc == null) ? TC_DEFAULT : Shared.tc;
    }
    
    /**
     * Gets the current version of game.
     *
     * @return The current version of the game using the format "x.y.z",
     *         where "x" is major, "y" is minor and "z" is revision.
     */
    public static String getVersion()
    {
        return Shared.FREECOL_VERSION;
    }

    /**
     * Gets the FreeColTcFile for the current TC.
     *
     * @return The <code>FreeColTcFile</code>.
     */
    public static FreeColTcFile getTCFile() {
        try {
            return new FreeColTcFile(getTC());
        } catch (IOException ioe) {}
        return null;
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

    /**
     * Gets the timeout.
     * Use the command line specified one if any, otherwise default
     * to `infinite' in single player and the TIMEOUT_DEFAULT for
     * multiplayer.
     *
     * @param singlePlayer True if this is a single player game.
     * @return A suitable timeout value.
     */
    public static int getTimeout(boolean singlePlayer) {
        return (Shared.timeout >= Shared.TIMEOUT_MIN) ? Shared.timeout
            : (singlePlayer) ? Integer.MAX_VALUE
            : TIMEOUT_DEFAULT;
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
     * Utility to make a load failure message.
     *
     * @param file The <code>File</code> that failed to load.
     * @return A <code>StringTemplate</code> with the error message.
     */
    public static StringTemplate badLoad(File file) {
        return StringTemplate.template("error.couldNotLoad")
            .addName("%name%", file.getPath());
    }

    /**
     * Utility to make a save failure message.
     *
     * @param file The <code>File</code> that failed to save.
     * @return A <code>StringTemplate</code> with the error message.
     */
    public static StringTemplate badSave(File file) {
        return StringTemplate.template("error.couldNotSave")
            .addName("%name%", file.getPath());
    }

    /**
     * We get a lot of lame bug reports with insufficient configuration
     * information.  Get a buffer containing as much information as we can
     * to embed in the log file and saved games.
     *
     * @return A <code>StringBuilder</code> full of configuration information.
     */
    public static StringBuilder getConfiguration() {
        File autosave = FreeColDirectories.getAutosaveDirectory();
        File clientOptionsFile = FreeColDirectories.getClientOptionsFile();
        File save = FreeColDirectories.getSaveDirectory();
        File userConfig = FreeColDirectories.getUserConfigDirectory();
        File userData = FreeColDirectories.getUserDataDirectory();
        File userMods = FreeColDirectories.getUserModsDirectory();
        StringBuilder sb = new StringBuilder(256);
        sb.append("Configuration:")
            .append("\n  version     ").append(getRevision())
            .append("\n  java:       ").append(JAVA_VERSION)
            .append("\n  memory:     ").append(MEMORY_MAX)
            .append("\n  locale:     ").append(locale)
            .append("\n  data:       ")
            .append(FreeColDirectories.getDataDirectory().getPath())
            .append("\n  userConfig: ")
            .append((userConfig == null) ? "NONE" : userConfig.getPath())
            .append("\n  userData:   ")
            .append((userData == null) ? "NONE" : userData.getPath())
            .append("\n  autosave:   ")
            .append((autosave == null) ? "NONE" : autosave.getPath())
            .append("\n  logFile:    ")
            .append(FreeColDirectories.getLogFilePath())
            .append("\n  options:    ")
            .append((clientOptionsFile == null) ? "NONE"
                : clientOptionsFile.getPath())
            .append("\n  save:       ")
            .append((save == null) ? "NONE" : save.getPath())
            .append("\n  userMods:   ")
            .append((userMods == null) ? "NONE" : userMods.getPath());
        return sb;
    }


    // The major final actions.

    /**
     * Start a client.
     *
     * @param userMsg An optional user message key.
     */
    private static void startClient(String userMsg)
    {
        Specification spec = null;
        File savegame = FreeColDirectories.getSavegameFile();
        if (Shared.debugStart)
        {
            spec = FreeCol.getTCSpecification();
        }
        
        else if (Shared.fastStart)
        {
            if (savegame == null)
            {
                // continue last saved game if possible,
                // otherwise start a new one
                savegame = FreeColDirectories.getLastSaveGameFile();
                if (savegame == null)
                {
                    spec = FreeCol.getTCSpecification();
                }
            }
            // savegame was specified on command line
        }
        final FreeColClient freeColClient
            = new FreeColClient(Shared.splashStream, Shared.fontName, Shared.guiScale, Shared.headless);
        freeColClient.startClient(Shared.windowSize, userMsg, Shared.sound, Shared.introVideo,
                                  savegame, spec);
    }

    /**
     * Start the server.
     */
    private static void startServer() {
        Shared.logger.info("Starting stand-alone server.");
        final FreeColServer freeColServer;
        File saveGame = FreeColDirectories.getSavegameFile();
        if (saveGame != null) {
            try {
                final FreeColSavegameFile fis
                    = new FreeColSavegameFile(saveGame);
                freeColServer = new FreeColServer(fis, (Specification)null,
                                                  Shared.serverPort, Shared.serverName);
                if (Shared.checkIntegrity) {
                    boolean integrityOK = freeColServer.getIntegrity() > 0;
                    gripe((integrityOK)
                        ? "cli.check-savegame.success"
                        : "cli.check-savegame.failure");
                    System.exit((integrityOK) ? 0 : 2);
                }
            } catch (Exception e) {
                if (Shared.checkIntegrity) gripe("cli.check-savegame.failure");
                fatal(Messages.message(badLoad(saveGame))
                    + ": " + e.getMessage());
                return;
            }
        } else {
            Specification spec = FreeCol.getTCSpecification();
            try {
                freeColServer = new FreeColServer(Shared.publicServer, false, spec,
                                                  Shared.serverPort, Shared.serverName);
            } catch (Exception e) {
                fatal(Messages.message("server.initialize")
                    + ": " + e.getMessage());
                return;
            }
            if (Shared.publicServer && freeColServer != null
                && !freeColServer.getPublicServer()) {
                gripe(Messages.message("server.noRouteToServer"));
            }
        }

        String quit = FreeCol.SERVER_THREAD + "Quit Game";
        Runtime.getRuntime().addShutdownHook(new Thread(quit) {
                @Override
                public void run() {
                    freeColServer.getController().shutdown();
                }
            });
    }
}
