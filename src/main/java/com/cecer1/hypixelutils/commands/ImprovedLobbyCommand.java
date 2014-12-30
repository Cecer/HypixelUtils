package com.cecer1.hypixelutils.commands;

import com.cecer1.hypixelutils.HypixelUtilsCore;
import com.cecer1.hypixelutils.UtilityMethods;
import com.cecer1.modframework.common.commands.AbstractedCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ImprovedLobbyCommand extends AbstractedCommand {

    public ImprovedLobbyCommand(String commandName) {
        super(commandName);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException
    {
        if(args.length == 0)
        {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/lobby");
            return;
        }

        if(args.length >= 2)
        {
            HypixelUtilsCore.improvedLobbyCommandProcessor.setDesiredLobbyNumber(Integer.parseInt(args[1]));
        }
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/lobby " + args[0]);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        if(!UtilityMethods.isHypixel())
            return false;
        return true;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
    {
        List<String> results = new ArrayList<String>();
        if(args.length == 1)
        {
            for(String lobbyType : HypixelUtilsCore.config.getLobbyTypes())
            {
                if(lobbyType.startsWith(args[0]))
                    results.add(lobbyType);
            }
        }
        if(results.isEmpty())
            return null;
        return results;
    }

    @Override
    public int getMaximumArgumentCount() {
        return 2;
    }
}
