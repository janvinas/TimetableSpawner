package io.github.janvinas.timetablespawner;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.sl.API.Variables;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartGroupStore;
import com.bergerkiller.bukkit.tc.properties.CartProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.intelie.omnicron.Cron;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.bergerkiller.bukkit.tc.signactions.spawner.SpawnSign;
import com.bergerkiller.bukkit.common.BlockLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class TimetableSpawner extends JavaPlugin {

    HashMap<String, Block> trainList = new HashMap<>();
    MapDisplay mapDisplay;
    HashMap<String, List<String>> departureBoards = new HashMap<>();
    int boardLength = 3;

    @Override
    public void onEnable (){
        getServer().getConsoleSender().sendMessage("TimetableSpawner enabled!");
        this.saveDefaultConfig();
        int trainDestroyDelay = this.getConfig().getInt("destroy-trains");
        
        for(String board : Objects.requireNonNull(getConfig().getConfigurationSection("departure-boards")).getKeys(false)){
            departureBoards.put(board, getConfig().getStringList("departure-boards." + board));
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8176), 0);
            server.createContext("/gettrains", new RequestHandler());
            server.setExecutor(null); // creates a default executor
            server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(trainDestroyDelay != 0){
            getServer().getScheduler().scheduleSyncRepeatingTask(this,() ->{
                for(String train : trainList.keySet()){
                    Collection<MinecartGroup> trainMatches = MinecartGroupStore.matchAll(train);
                    for(MinecartGroup matchingTrain : trainMatches){
                        if( (!matchingTrain.isUnloaded()) && (trainList.get(train).equals(matchingTrain.get(0).getBlock())) ){

                            BlockLocation loc = matchingTrain.getProperties().getLocation();
                            matchingTrain.destroy();
                            getLogger().info("Train " + train + " at " + loc.x + "," + loc.y + "," + loc.z + " has been destroyed");
                        }
                    }
                }

                trainList.clear();
                for(MinecartGroup train : MinecartGroupStore.getGroups()){
                    trainList.put(train.getProperties().getTrainName(), train.get(0).getBlock());
                }
            } , 0, trainDestroyDelay);
        }

        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            TreeMap<LocalDateTime, String> departureBoardTrains = new TreeMap<>();
            LocalDateTime now = LocalDateTime.now();
            for(String boardName : departureBoards.keySet()){
                departureBoardTrains.clear();
                for(String trainLine : departureBoards.get(boardName)){
                    StringTokenizer stringTokenizer = new StringTokenizer(trainLine, "-");
                    String trainLineName = stringTokenizer.nextToken();
                    String trainLineCronExpression = stringTokenizer.nextToken();
                    Cron cron = new Cron(trainLineCronExpression);

                    LocalDateTime input = now;
                    //put enough trains of every train line so the board will never be empty
                    for (int i = 0; i < boardLength; i++) {
                        input = cron.next(input);
                        departureBoardTrains.put(input, trainLineName);
                    }
                }

                //save board variables
                int j = 0;
                for(LocalDateTime departureTime : departureBoardTrains.keySet()){
                    if(j < boardLength){
                        //format is: "board1N" (train name) and "board1T" (departure time)
                        Variables.get(boardName + "-" + j + "T").set(departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        Variables.get(boardName + "-" + j + "N").set(departureBoardTrains.get(departureTime));
                    }
                    j++;
                }
            }
        }, 0, 100);

    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("timetablespawner")) {

            if(args.length == 5 && args[0].equalsIgnoreCase("spawn")){
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
            }else if(args.length == 1 && args[0].equalsIgnoreCase("trainlist")){
                sender.sendMessage(getTrains());
            }else if(args.length == 1 && args[0].equalsIgnoreCase("boardlist")){
                sender.sendMessage(departureBoards.toString());
            }
        }
        return false;
    }

    void sendInformation(String tag, CommandSender sender){
        sender.sendMessage(ChatColor.AQUA + "Train Information:");
        tag = tag.substring(tag.indexOf('_') + 1);      //delete the first part of the string
        while(tag.indexOf('_') != -1){
            sender.sendMessage(ChatColor.AQUA + tag.substring(0, tag.indexOf(';')));
            tag = tag.substring(tag.indexOf('_') + 1);  //delete the next part of the string that we just printed
        }
        sender.sendMessage(ChatColor.AQUA + tag);        //send the last part of the string
    }

    static class RequestHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            byte [] response = getTrains().getBytes();
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private static String getTrains(){
        String trains = "{";
        for(MinecartGroup group : MinecartGroupStore.getGroups()){
            String trainName = group.getProperties().getTrainName();
            String trainWorld = group.getWorld().getName();
            String coordX = String.valueOf(group.get(0).getBlock().getLocation().getX());
            String coordY = String.valueOf(group.get(0).getBlock().getLocation().getY());
            String coordZ = String.valueOf(group.get(0).getBlock().getLocation().getZ());
            String coords = trainWorld + ":" + coordX + "," + coordY + "," + coordZ;
            trains = trains.concat("\"" + trainName + "\": \"" + coords + "\",");
        }
        if(trains.length() > 10) trains = trains.substring(0, trains.length() - 1);
        trains = trains.concat("}");
        return trains;
    }

}

