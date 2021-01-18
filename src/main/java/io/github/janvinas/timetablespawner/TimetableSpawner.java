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
        }
        return false;
    }
}
