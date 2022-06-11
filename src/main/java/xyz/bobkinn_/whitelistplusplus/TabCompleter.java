package xyz.bobkinn_.whitelistplusplus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<? extends Player> plist = new ArrayList<Player>(sender.getServer().getOnlinePlayers());

        List<String> onlinePlayers = new ArrayList<>();
        for (Player p: plist){
            onlinePlayers.add(p.getName());
        }

        if (args.length==1) {
            return Arrays.asList("on","off","list","add","del","rl");
        }
        if (args.length==2 && args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("del")) {
//            if (args[0].equalsIgnoreCase("del")){
//                return plugin.getWhitelistUsers(sender);
//            }
            if (args.length>2){return Collections.emptyList();}
            return onlinePlayers;
        }
        return Collections.emptyList();
    }
}
