package at.Xirado.Bean.Main;

import at.Xirado.Bean.CommandManager.CommandManager;
import at.Xirado.Bean.CommandManager.ConsoleCommandManager;
import at.Xirado.Bean.Handlers.*;
import at.Xirado.Bean.Logging.Console;
import at.Xirado.Bean.Logging.Shell;
import at.Xirado.Bean.Misc.JSONConfig;
import at.Xirado.Bean.Misc.SQL;
import at.Xirado.Bean.Misc.Util;
import at.Xirado.Bean.PunishmentManager.Case;
import at.Xirado.Bean.PunishmentManager.CaseType;
import at.Xirado.Bean.PunishmentManager.Punishments;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.Listener;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
public class DiscordBot
{
    public static final long OWNER_ID = 184654964122058752L;
    public static final long STARTTIME = System.currentTimeMillis()/1000;

    public static DiscordBot instance;

    public static DiscordBot getInstance()
    {
        return instance;
    }

    private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Threadpool (Thread %d)").build();
    public final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);

    public final PrefixManager prefixManager = new PrefixManager();
    public final LogChannelManager logChannelManager = new LogChannelManager();
    public final BlacklistManager blacklistManager = new BlacklistManager();
    public final ReactionRoleManager reactionRoleManager = new ReactionRoleManager();
    public final PermissionCheckerManager permissionCheckerManager;
    public final MutedRoleManager mutedRoleManager;


    public Bot musicinstance;
    private String token;
    public final String path;
    public JDA jda;
    public final CommandManager commandManager;
    public final ConsoleCommandManager consoleCommandManager;
    public static boolean debugMode;

    public final String VERSION;

    public static Logger logger =  (Logger) LoggerFactory.getLogger("ROOT");

    public DiscordBot()
    {
        instance = this;
        Thread.currentThread().setName("Main-Thread");
        final Properties properties = new Properties();
        try
        {
            properties.load(this.getClass().getClassLoader().getResourceAsStream("settings.properties"));
        } catch (IOException e)
        {
            Console.logger.error("Could not retrieve project metadata!", e);
        }
        VERSION = properties.getProperty("app-version");
        Shell.startShell();
        while(!Shell.startedSuccessfully)
        {
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                Console.logger.error("", e);
            }
        }
        Util.setLoggingLevel(Level.INFO);
        Console.info("Logging level set to \"INFO\"");
        File file = new File("token.txt");
        if(!file.exists())
        {
            try
            {
                file.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            logger.warn("token.txt has been created. Please add your Discord Bot Token to it, save and start again!");
            System.exit(1);
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            token = br.readLine();
            br.close();
        } catch (IOException e)
        {
            logger.error(ExceptionUtils.getStackTrace(e));
        }

        this.path = Util.getPath();
        JSONConfig.updateConfig();
        SQL.connect();
        SQL.initKeepAlive();
        if(!SQL.isConnected())
        {
            logger.error("I can't run without a SQL connection!");
            System.exit(1);
        }
        SQLHelper.createTables();
        permissionCheckerManager = new PermissionCheckerManager();
        jda = null;
        try
        {
            jda = JDABuilder.create(token, EnumSet.allOf(GatewayIntent.class)).setMemberCachePolicy(MemberCachePolicy.ONLINE).build();
        } catch (LoginException e)
        {
            logger.error(ExceptionUtils.getStackTrace(e));
        }

        addShutdownHook();
        try
        {
            this.jda.awaitReady();
        } catch (InterruptedException e)
        {
            System.exit(0);
        }
        logger.info("Successfully logged in as @"+this.jda.getSelfUser().getAsTag());
        this.jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("bean.bz | +help"), false);
        Util.addListeners();

        commandManager = new CommandManager();
        commandManager.registerAllCommands();
        consoleCommandManager = new ConsoleCommandManager();
        consoleCommandManager.registerAllCommands();
        mutedRoleManager = new MutedRoleManager();
        BotConfig config = new BotConfig(null);
        config.load();
        if(!config.isValid())
            return;

        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        this.musicinstance = new Bot(waiter, config, settings);

        this.musicinstance.setJDA(instance.jda);
        this.jda.addEventListener(waiter);
        this.jda.addEventListener(new Listener(this.musicinstance));
        scheduledExecutorService.submit(() ->
        {
            String qry = "SELECT * FROM modCases WHERE active = 1 AND duration > 0";
            try
            {
                Connection connection = SQL.getConnectionFromPool();
                PreparedStatement ps = connection.prepareStatement(qry);
                ResultSet rs = ps.executeQuery();
                List<Case> cases = new ArrayList<>();
                while(rs.next())
                {
                    cases.add(new Case(CaseType.valueOf(rs.getString("caseType").toUpperCase()), rs.getLong("guildID"), rs.getLong("targetID"), rs.getLong("moderatorID"), rs.getString("reason"), rs.getLong("duration"), rs.getLong("creationDate"), rs.getString("caseID"), rs.getBoolean("active")));

                }
                connection.close();
                for(Case modcase : cases)
                {
                    if (modcase.getType() == CaseType.TEMPBAN)
                    {
                        if(modcase.getCreatedAt()+modcase.getDuration() < System.currentTimeMillis())
                        {
                            try
                            {
                                Guild g = instance.jda.getGuildById(modcase.getGuildID());
                                if(g == null)
                                {
                                    modcase.setActive(false);
                                    continue;
                                }
                                Punishments.unban(modcase, null);
                                continue;
                            }catch (Exception e)
                            {
                                logger.error("Could not undo punishment", e);
                                continue;
                            }
                        }
                        Runnable r = () ->
                        {
                            Punishments.unban(modcase, null);
                        };
                        scheduledExecutorService.schedule(r, (modcase.getDuration()+modcase.getCreatedAt())-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    }else if(modcase.getType() == CaseType.MUTE)
                    {
                        if(modcase.getCreatedAt()+modcase.getDuration() < System.currentTimeMillis())
                        {
                            try
                            {
                                Guild g = instance.jda.getGuildById(modcase.getGuildID());
                                if(g == null)
                                {
                                    modcase.setActive(false);
                                    continue;
                                }

                                Punishments.unmute(modcase, null);
                                continue;
                            }catch (Exception e)
                            {
                                logger.error("Could not undo punishment", e);
                                continue;
                            }
                        }
                        Runnable r = () ->
                        {
                            Punishments.unmute(modcase, null);
                        };
                        scheduledExecutorService.schedule(r, (modcase.getDuration()+modcase.getCreatedAt())-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            } catch (SQLException throwables)
            {
                logger.error("An error occured", throwables);
            }
        });
        logger.info("Successfully started up!");
        debugMode = runningFromIntelliJ();
        if(debugMode)
        {
            logger.warn("Debug mode enabled! Not listening for any user-commands.");
            getInstance().jda.getPresence().setPresence(OnlineStatus.INVISIBLE,false);
        }
    }

    public static void main(String[] args) throws Exception {
        new DiscordBot();

    }

    public static boolean runningFromIntelliJ()
    {
        return Boolean.parseBoolean(JSONConfig.config.get("Debug"));
    }

    public static void addShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            logger.info("Shutting down...");
            getInstance().scheduledExecutorService.shutdown();
            getInstance().jda.shutdown();
        }));
    }
}
