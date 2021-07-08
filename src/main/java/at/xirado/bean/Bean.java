package at.xirado.bean;

import at.xirado.bean.command.ConsoleCommandManager;
import at.xirado.bean.command.handler.CommandHandler;
import at.xirado.bean.command.handler.SlashCommandHandler;
import at.xirado.bean.data.DataObject;
import at.xirado.bean.data.database.Database;
import at.xirado.bean.event.*;
import at.xirado.bean.log.Shell;
import at.xirado.bean.misc.Util;
import at.xirado.bean.music.AudioManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Bean.class);
    private static Bean instance;
    private static final String VERSION = loadVersion();
    public static final long OWNER_ID = 184654964122058752L;
    public static final long START_TIME = System.currentTimeMillis() / 1000;

    private final JDA jda;
    private final DataObject config = loadConfig();
    private final boolean debug;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactoryBuilder().setNameFormat("Bean Thread %d").build());
    private final ConsoleCommandManager consoleCommandManager;
    private final SlashCommandHandler slashCommandHandler;
    private final CommandHandler commandHandler;
    private final AudioManager audioManager;
    private final EventWaiter eventWaiter;

    public Bean() throws Exception
    {
        instance = this;
        Database.connect();
        Database.awaitReady();
        consoleCommandManager = new ConsoleCommandManager();
        consoleCommandManager.registerAllCommands();
        debug = config.getBoolean("debug");
        slashCommandHandler = new SlashCommandHandler();
        commandHandler = new CommandHandler();
        eventWaiter = new EventWaiter();
        Class.forName("at.xirado.bean.translation.LanguageLoader");
        jda = JDABuilder.create(config.getString("token"), getIntents())
                .setMemberCachePolicy(MemberCachePolicy.ONLINE.and(MemberCachePolicy.VOICE))
                .setActivity(Activity.watching("www.bean.bz"))
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(new OnReadyEvent(), new OnSlashCommand(), new OnGuildMessageReceived(),
                        new OnGainXP(), new OnGuildMessageReactionAdd(), new OnGuildMessageReactionRemove(), new OnVoiceUpdate(),
                        eventWaiter)
                .build();
        audioManager = new AudioManager(jda);
    }

    public static Bean getInstance()
    {
        return instance;
    }

    public DataObject getConfig()
    {
        return config;
    }

    public ScheduledExecutorService getExecutor()
    {
        return scheduledExecutorService;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public JDA getJDA()
    {
        return jda;
    }

    public ConsoleCommandManager getConsoleCommandManager()
    {
        return consoleCommandManager;
    }

    public SlashCommandHandler getSlashCommandHandler()
    {
        return slashCommandHandler;
    }

    public CommandHandler getCommandHandler()
    {
        return commandHandler;
    }

    public AudioManager getAudioManager()
    {
        return audioManager;
    }

    public EventWaiter getEventWaiter()
    {
        return eventWaiter;
    }

    public static void main(String[] args)
    {
        Thread.currentThread().setName("Main-Thread");
        try
        {
            Shell.startShell();
            Shell.awaitReady();
            new Bean();
        } catch (Exception e)
        {
            if (e instanceof LoginException ex)
            {
                LOGGER.error("Could not login to Discord!", ex);
                return;
            }
            LOGGER.error("An error occured starting Bean!", e);
        }
    }


    private DataObject loadConfig()
    {
        File configFile = new File("config.json");
        if (!configFile.exists())
        {
            InputStream inputStream = Bean.class.getResourceAsStream("/config.json");
            if (inputStream == null)
            {
                LOGGER.error("Could not copy config from resources folder!");
                return DataObject.empty();
            }
            Path path = Paths.get(Util.getJarPath() + "/config.json");
            System.out.println(path.toAbsolutePath());
            try
            {
                Files.copy(inputStream, path);
            } catch (IOException e)
            {
                LOGGER.error("Could not copy config file!", e);
            }
        }
        return DataObject.parse(configFile);
    }

    private static String loadVersion()
    {
        try
        {
            Properties properties = new Properties();
            properties.load(Bean.class.getClassLoader().getResourceAsStream("settings.properties"));
            return properties.getProperty("app-version");
        } catch (IOException e)
        {
            LOGGER.error("Could not get version!", e);
            return "0.0.0";
        }
    }

    private static Set<GatewayIntent> getIntents()
    {
        return Set.of(GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS);
    }

    public static void info(String msg)
    {
        LOGGER.info(msg);
    }

    public static void warn(String msg)
    {
        LOGGER.warn(msg);
    }

    public static void error(String msg)
    {
        LOGGER.error(msg);
    }

    public static void error(String msg, Throwable t)
    {
        LOGGER.error(msg, t);
    }

    public static String getBeanVersion()
    {
        return VERSION;
    }
}
