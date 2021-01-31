package io.github.janvinas.timetablespawner;

import com.bergerkiller.bukkit.tc.properties.CartProperties;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.bergerkiller.bukkit.tc.signactions.spawner.SpawnSign;
import com.bergerkiller.bukkit.common.BlockLocation;
import java.util.Set;


public class TimetableSpawner extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage("TimetableSpawner enabled!");
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("timetablespawner")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("signinfo")) {
                Player p = (Player) sender;
                SpawnSign targeted = new SpawnSign(new BlockLocation(p.getTargetBlock(null, 10).getLocation()));
                sender.sendMessage(ChatColor.AQUA + "You are editing the sign:" + targeted.toString());
                return true;
            }else if(args.length == 5 && args[0].equalsIgnoreCase("locationinfo")){
                Location location = new Location(getServer().getWorld(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                sender.sendMessage(ChatColor.AQUA + "You are editing the sign:" + new BlockLocation(location).toString());
                return true;
            }else if(args.length == 5 && args[0].equalsIgnoreCase("spawn")){
                Location location = new Location(getServer().getWorld(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                new SpawnSign(new BlockLocation(location)).spawn();
                return true;
            }else if(args.length == 1 && args[0].equalsIgnoreCase("spawntargeting")){
                Player p = (Player) sender;
                SpawnSign targeted = new SpawnSign(new BlockLocation(p.getTargetBlock(null, 10).getLocation()));
                targeted.spawn();
                return true;
            }else if(args.length == 1 && args[0].equalsIgnoreCase("info")){
                Set<String> tags = CartProperties.getEditing((Player) sender).getTags();
                for (String tag : tags){
                    if(tag.startsWith("ts_")){
                        sendInformation(tag, sender);
                    }
                }
                return true;
            }
        }
        return false;
    }

    void sendInformation(String tag, CommandSender sender){
        sender.sendMessage(ChatColor.AQUA + "Train Information:");
        tag = tag.substring(tag.indexOf('_') + 1);      //delete the first part of the string
        while(tag.indexOf('_') != -1){
            sender.sendMessage(tag.substring(0, tag.indexOf(';')));
            tag = tag.substring(tag.indexOf('_') + 1);  //delete the next part of the string that we just printed
        }
        sender.sendMessage(tag);        //send the last part of the string
    }
}

