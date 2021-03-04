package at.Xirado.Bean.Commands.Moderation;

import at.Xirado.Bean.CommandManager.Command;
import at.Xirado.Bean.CommandManager.CommandEvent;
import at.Xirado.Bean.CommandManager.CommandType;
import at.Xirado.Bean.Main.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

public class SetMutedRoleCommand extends Command
{
    public SetMutedRoleCommand(JDA jda)
    {
        super(jda);
        this.invoke = "setmutedrole";
        this.commandType = CommandType.ADMIN;
        this.neededPermissions = Collections.singletonList(Permission.ADMINISTRATOR);
        this.description = "Sets the role given to muted members";
        this.usage = "setmutedrole [@Role/ID]";
    }

    @Override
    public void executeCommand(CommandEvent event)
    {
        String[] args = event.getArguments().toStringArray();
        if(args.length != 1)
        {
            event.replyErrorUsage();
            return;
        }

        String roleID = args[0].replaceAll("[^0-9]", "");
        if(roleID.length() == 0)
        {
            event.replyError("Role-ID may not be empty!");
            return;
        }
        Guild g = event.getGuild();
        Role role = g.getRoleById(roleID);
        if(role == null)
        {
            event.replyError("This is not a valid role!");
            return;
        }
        if(!event.getSelfMember().canInteract(role))
        {
            event.replyError("I cannot interact with this role!");
            return;
        }
        DiscordBot.getInstance().mutedRoleManager.setMutedRole(g.getIdLong(), role.getIdLong());
        event.reply(new EmbedBuilder()
            .setColor(Color.green)
            .setDescription(role.getAsMention()+" is now the \"muted\"-role!\n\n(Make sure you have setup this role correctly, so users who are muted cannot write in any chat!)")
            .build()
        );
    }
}
