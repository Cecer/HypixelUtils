package com.cecer1.hypixelutils.commands;

import com.cecer1.hypixelutils.HypixelUtilsCore;
import com.cecer1.hypixelutils.UtilityMethods;
import com.cecer1.modframework.common.commands.AbstractedCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class LobbyProtectionToggleCommand extends AbstractedCommand {

    public LobbyProtectionToggleCommand(String commandName) {
        super(commandName);
    }

    @Override
    public void processCommand(ICommandSender iCommandSender, String[] strings) throws CommandException
    {
        IChatComponent commandReply = UtilityMethods.getHypixelUtilsChatComponentPrefix()
                .appendSibling(new ChatComponentText("Bypassing /lobby protection has been ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));

        if(HypixelUtilsCore.config.isBypassLobbyProtectionEnabled())
        {
            HypixelUtilsCore.config.setBypassLobbyProtectionEnabled(false);
            commandReply.appendSibling(new ChatComponentText("DISABLED").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
        }
        else
        {
            HypixelUtilsCore.config.setBypassLobbyProtectionEnabled(true);
            commandReply.appendSibling(new ChatComponentText("ENABLED").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        }
        commandReply.appendSibling(new ChatComponentText(".").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));

        Minecraft.getMinecraft().thePlayer.addChatMessage(commandReply);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender iCommandSender)
    {
        if(!UtilityMethods.isHypixel())
            return false;
        return true;
    }

    @Override
    public int getMaximumArgumentCount() {
        return 0;
    }
}
