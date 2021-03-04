package at.Xirado.Bean.Commands;

import at.Xirado.Bean.CommandManager.Command;
import at.Xirado.Bean.CommandManager.CommandEvent;
import at.Xirado.Bean.CommandManager.CommandType;
import at.Xirado.Bean.Misc.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;

public class Haste extends Command
{
    public Haste(JDA jda)
    {
        super(jda);
        this.invoke = "haste";
        this.aliases = Arrays.asList("posthaste", "hastebin");
        this.commandType = CommandType.UTILITIES;
        this.description = "Creates a haste and gives you the link";
        this.usage = "haste [Content]";
    }

    @Override
    public void executeCommand(CommandEvent event)
    {
        String[] args = event.getArguments().toStringArray();
        if(args.length == 0)
        {
            event.replyErrorUsage();
            return;
        }
        event.deleteInvokeMessage();
        String haste = event.getArguments().getAsString(0);
        String linkresponse = Util.postHaste(haste, false);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode("#93bdca"))
                .setFooter("Hastebin")
                .setTimestamp(Instant.now())
                .setAuthor(event.getAuthor().getAsTag(), null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription("Your haste is finished: "+linkresponse);
        event.reply(builder.build());

    }
}
