// Big Thanks to VolcAddons, most of the codes are from them

package com.github.tunatuna1733.takeshiupdater;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandUpdateTakeshi extends CommandBase {
    @Override
    public String getCommandName() {
        return "updatetakeshi";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/updatetakeshi";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            player.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[" +
                            EnumChatFormatting.GREEN + "TakeshiUpdater" +
                            EnumChatFormatting.GOLD + "] " +
                            EnumChatFormatting.AQUA + "Updating TakeshiAddons..."
            ));
            TakeshiUpdater.INSTANCE.downloadAndExtractUpdate(player);
        }
    }
}
