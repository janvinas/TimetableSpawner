package io.github.janvinas.timetablespawner;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.bergerkiller.bukkit.tc.signactions.spawner.SpawnSign;
import com.bergerkiller.bukkit.common.BlockLocation;

public class TimetableSpawner extends JavaPlugin {

    SpawnSign spawner;

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage("TimetableSpawner enabled!");
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("timetablespawner") && args.length == 1){
            if(args[0].equalsIgnoreCase("register")){
                Player player = (Player) sender;
                spawner = new SpawnSign(new BlockLocation(player.getTargetBlock(null, 10).getLocation()));
                return true;
            }else if(args[0].equalsIgnoreCase("spawn")){
                spawner.spawn();
                return true;
            }
        }else if(command.getName().equalsIgnoreCase("timetablespawner") && args.length == 2){
            if(args[0].equalsIgnoreCase("schedule")){
                if (Integer.parseInt(args [1]) > 0) {
                    int ticks = Integer.parseInt(args[1]);
                    getServer().getScheduler().runTaskLater(this, () -> spawner.spawn(), ticks);
                }
            }
        }
        return false;
    }

}
