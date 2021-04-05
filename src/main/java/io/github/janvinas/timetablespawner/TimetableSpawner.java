package io.github.janvinas.timetablespawner;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.sl.API.TickMode;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartGroupStore;
import com.bergerkiller.bukkit.tc.properties.CartProperties;
import com.bergerkiller.bukkit.tc.signactions.spawner.SpawnSign;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class TimetableSpawner extends JavaPlugin {

    HashMap<String, Block> trainList = new HashMap<>();
    HashMap<String, List<String>> departureBoards = new HashMap<>();
    static HashMap<String, List<String>> displayDepartureBoards = new HashMap<>();
    static int secondsToDisplayOnBoard = 20;
    boolean blinkPlatformNumber = false;
    boolean blinkNow = false;

    int trainDestroyDelay;
    String dontDestroyTag;

    @Override
    public void onEnable (){
        getServer().getConsoleSender().sendMessage("TimetableSpawner enabled!");
        this.saveDefaultConfig();
        trainDestroyDelay = getConfig().getInt("destroy-trains");
        secondsToDisplayOnBoard = getConfig().getInt("seconds-to-display-on-board");
        dontDestroyTag = getConfig().getString("dont-destroy-tag");
        blinkPlatformNumber = getConfig().getBoolean("blink-platform-number");
        blinkNow = getConfig().getBoolean("blink-now");

        for(String board : Objects.requireNonNull(getConfig().getConfigurationSection("departure-boards")).getKeys(false)){
            departureBoards.put(board, getConfig().getStringList("departure-boards." + board));
        }
        for(String board : Objects.requireNonNull(getConfig().getConfigurationSection("display-departure-boards")).getKeys(false)){
            displayDepartureBoards.put(board, getConfig().getStringList("display-departure-boards." + board));
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

                            if(!matchingTrain.getProperties().matchTag(dontDestroyTag)){
                                BlockLocation loc = matchingTrain.getProperties().getLocation();
                                matchingTrain.destroy();
                                getLogger().info("Train " + train + " at " + loc.x + "," + loc.y + "," + loc.z + " has been destroyed");
                            }
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
            TreeMap<LocalDateTime, TrainInformation> departureBoardTrains = new TreeMap<>();
            LocalDateTime now = LocalDateTime.now();

            for(String boardName : departureBoards.keySet()){
                StringTokenizer boardNameTokenizer = new StringTokenizer(boardName, "|");
                String parsedBoardName = boardNameTokenizer.nextToken();
                int boardLength = Integer.parseInt(boardNameTokenizer.nextToken());

                departureBoardTrains.clear();
                departureBoardTrains = BoardUtils.fillDepartureBoard(now, departureBoards.get(boardName), boardLength, true);

                //save board variables
                int i = 0;
                for(LocalDateTime departureTime : departureBoardTrains.keySet()){
                    //format is: "<boardName>-<lineNumber><variableType>"
                    Duration untilDeparture = Duration.between(now, departureTime);
                    if(untilDeparture.minusSeconds(secondsToDisplayOnBoard).isNegative()){
                        Variables.get(parsedBoardName + "-" + i + "T").set("now");
                        if(blinkNow){
                            Variable var = Variables.get(parsedBoardName + "-" + i + "T");
                            var.getTicker().setMode(TickMode.BLINK);
                            var.getTicker().setInterval(10);
                        }
                        if(blinkPlatformNumber){
                            Variable var = Variables.get(parsedBoardName + "-" + i + "P");
                            var.getTicker().setMode(TickMode.BLINK);
                            var.getTicker().setInterval(500);
                        }
                    }else if(untilDeparture.minusMinutes(5).isNegative()){
                        Variables.get(parsedBoardName + "-" + i + "T").set(
                                departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " (" +
                                        (int) untilDeparture.getSeconds() / 60 + "min)");
                        Variables.get(parsedBoardName + "-" + i + "T").getTicker().setMode(TickMode.NONE);
                        Variables.get(parsedBoardName + "-" + i + "P").getTicker().setMode(TickMode.NONE);
                    }else{
                        Variables.get(parsedBoardName + "-" + i + "T").set(departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }
                    Variables.get(parsedBoardName + "-" + i + "N").set(departureBoardTrains.get(departureTime).name);
                    String destination = departureBoardTrains.get(departureTime).destination;
                    if(!destination.equals("_")) Variables.get(parsedBoardName + "-" + i + "D").set(destination);
                    String platform = departureBoardTrains.get(departureTime).platform;
                    if(!platform.equals("_")) Variables.get(parsedBoardName + "-" + i + "P").set(platform);
                    String information = departureBoardTrains.get(departureTime).information;
                    if(!platform.equals("_")) Variables.get(parsedBoardName + "-" + i + "I").set(information);
                    i++;
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
                    if(tag.startsWith("ts|")){
                        sendInformation(tag, sender);
                    }
                }
                return true;
            }else if(args.length == 1 && args[0].equalsIgnoreCase("trainlist")){
                sender.sendMessage(getTrains());
                return true;
            }else if(args.length == 1 && args[0].equalsIgnoreCase("boardlist")){
                sender.sendMessage(departureBoards.toString());
                return true;
            }else if(args.length == 1  && args[0].equalsIgnoreCase("reload")){
                reloadConfig();
                blinkPlatformNumber = getConfig().getBoolean("blink-platform-number");
                blinkNow = getConfig().getBoolean("blink-now");
                trainDestroyDelay = getConfig().getInt("destroy-trains");
                dontDestroyTag = getConfig().getString("dont-destroy-tag");
                for(String board : Objects.requireNonNull(getConfig().getConfigurationSection("departure-boards")).getKeys(false)){
                    departureBoards.put(board, getConfig().getStringList("departure-boards." + board));
                }
                for(String board : Objects.requireNonNull(getConfig().getConfigurationSection("display-departure-boards")).getKeys(false)){
                    displayDepartureBoards.put(board, getConfig().getStringList("display-departure-boards." + board));
                }
                return true;
            }else if(args.length == 3 && args[0].equalsIgnoreCase("createdisplay")){
                ItemStack display = MapDisplay.createMapItem(MapDisplays.DepartureBoard1.class);
                ItemUtil.getMetaTag(display).putValue("id", args[1]);
                ItemUtil.getMetaTag(display).putValue("name", args[2]);
                ((Player) sender).getInventory().addItem(display);
                return true;
            }
        }
        return false;
    }

    void sendInformation(String tag, CommandSender sender){
        tag = ChatColor.translateAlternateColorCodes('&', tag);
        sender.sendMessage(ChatColor.AQUA.toString() + ChatColor.UNDERLINE + "Train Information:");
        StringTokenizer tokenizer = new StringTokenizer(tag, "|");
        tokenizer.nextToken();
        while(tokenizer.hasMoreTokens()){
            sender.sendMessage(tokenizer.nextToken());
        }
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

