package io.github.janvinas.timetablespawner;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapFont;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MapDisplays {

    public static class DepartureBoard1 extends MapDisplay{
        public HashMap<UUID, Integer> taskList = new HashMap<>();

        @Override
        public void onAttached() {

            getLayer().fillRectangle(0, 0, 256, 10, MapColorPalette.getColor(72, 129, 183));
            getLayer().fillRectangle(0, 10, 256, 1, MapColorPalette.getColor(0, 0, 0));
            getLayer().draw(MapFont.MINECRAFT, 1, 1, MapColorPalette.getColor(255, 255, 255), "Departures:");
            getLayer().setAlignment(MapFont.Alignment.RIGHT);
            getLayer().draw(MapFont.MINECRAFT, 254, 1, MapColorPalette.getColor(255, 255, 255), properties.get("name", String.class).replace('_', ' '));
            //draw background strips
            getLayer().fillRectangle(0, 11, 256, 10, MapColorPalette.getColor(255, 255, 255));
            getLayer().fillRectangle(0, 21, 256, 10, MapColorPalette.getColor(200, 200, 200));
            getLayer().fillRectangle(0, 31, 256, 10, MapColorPalette.getColor(255, 255, 255));
            getLayer().fillRectangle(0, 41, 256, 10, MapColorPalette.getColor(200, 200, 200));
            getLayer().fillRectangle(0, 51, 256, 10, MapColorPalette.getColor(255, 255, 255));
            getLayer().fillRectangle(0, 61, 256, 10, MapColorPalette.getColor(200, 200, 200));
            getLayer().fillRectangle(0, 71, 256, 10, MapColorPalette.getColor(255, 255, 255));
            getLayer().fillRectangle(0, 81, 256, 10, MapColorPalette.getColor(200, 200, 200));
            getLayer().fillRectangle(0, 91, 256, 10, MapColorPalette.getColor(255, 255, 255));
            getLayer().fillRectangle(0, 101, 256, 10, MapColorPalette.getColor(200, 200, 200));

            //draw column titles:
            getLayer().setAlignment(MapFont.Alignment.LEFT);
            getLayer().draw(MapFont.MINECRAFT, 1, 12, MapColorPalette.getColor(0, 0, 255), "Time");
            getLayer().draw(MapFont.MINECRAFT, 80, 12, MapColorPalette.getColor(0, 0, 255), "Line");
            getLayer().draw(MapFont.MINECRAFT, 107, 12, MapColorPalette.getColor(0, 0, 255), "Destination");
            getLayer().draw(MapFont.MINECRAFT, 170, 12, MapColorPalette.getColor(0, 0, 255), "Pl.");
            getLayer().draw(MapFont.MINECRAFT, 190, 12, MapColorPalette.getColor(0, 0, 255), "Information");
            //draw last line, where time and date will be shown
            getLayer().fillRectangle(0, 111, 256, 18, MapColorPalette.getColor(0, 0, 0));
            getLayer().fillRectangle(0, 112, 256, 10, MapColorPalette.getColor(72, 129, 183));

            //schedule task that will update the train list every 5 seconds
            BukkitScheduler scheduler = getPlugin().getServer().getScheduler();
            int taskId = scheduler.scheduleSyncRepeatingTask(getPlugin(), () ->{
                getLayer(1).clear();
                getLayer(1).setAlignment(MapFont.Alignment.LEFT);

                HashMap<String, List<String>> displayDepartureBoards = TimetableSpawner.displayDepartureBoards;
                int secondsToDisplayOnBoard = TimetableSpawner.secondsToDisplayOnBoard;

                String boardName = properties.get("id", String.class);
                StringTokenizer boardNameTokenizer = new StringTokenizer(boardName, "|");
                boardNameTokenizer.nextToken();
                int boardLength = Integer.parseInt(boardNameTokenizer.nextToken());

                LocalDateTime now = LocalDateTime.now();
                TreeMap<LocalDateTime, TrainInformation> departureBoardTrains = BoardUtils.fillDepartureBoard(now, displayDepartureBoards.get(boardName), boardLength, true);

                //print train lines on screen
                int i = 0;
                for(LocalDateTime departureTime : departureBoardTrains.keySet()){
                    Duration untilDeparture = Duration.between(now, departureTime);
                    if(untilDeparture.minusSeconds(secondsToDisplayOnBoard).isNegative()){
                        getLayer(1).draw(MapFont.MINECRAFT, 1, 22 + i*10,
                                MapColorPalette.getColor(255, 0, 0),
                                "now");
                    }else if(untilDeparture.minusMinutes(5).isNegative()){
                        getLayer(1).draw(MapFont.MINECRAFT, 1, 22 + i*10,
                                MapColorPalette.getColor(0, 0, 0),
                                departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " (" +
                                        (int) untilDeparture.getSeconds() / 60 + "min)");
                    }else{
                        getLayer(1).draw(MapFont.MINECRAFT, 1, 22 + i*10,
                                MapColorPalette.getColor(0, 0, 0),
                                departureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }

                    getLayer(1).draw(MapFont.MINECRAFT, 80, 22 + i*10,
                            MapColorPalette.getColor(0, 0, 0),
                            departureBoardTrains.get(departureTime).name);
                    String destination = departureBoardTrains.get(departureTime).destination;
                    if(!destination.equals("_")) getLayer(1).draw(MapFont.MINECRAFT, 107, 22 + i*10,
                                MapColorPalette.getColor(0, 0, 0),
                                departureBoardTrains.get(departureTime).destination);
                    String platform = departureBoardTrains.get(departureTime).platform;
                    if(!platform.equals("_")) getLayer(1).draw(MapFont.MINECRAFT, 170, 22 + i*10,
                            MapColorPalette.getColor(0, 0, 0),
                            departureBoardTrains.get(departureTime).platform);
                    String information = departureBoardTrains.get(departureTime).information;
                    if(!information.equals("_")) getLayer(1).draw(MapFont.MINECRAFT, 190, 22 + i*10,
                            MapColorPalette.getColor(0, 0, 0),
                            departureBoardTrains.get(departureTime).information);

                    i++;
                }

            }, 0, 100);

            taskList.put(properties.getUniqueId(), taskId);
            super.onAttached();
        }

        @Override
        public void onDetached() {
            getPlugin().getServer().getScheduler().cancelTask(taskList.get(properties.getUniqueId()));
            taskList.remove(info.uuid);
            super.onDetached();
        }

        @Override
        public void onTick() {
            getLayer(3).clear();
            getLayer(3).setAlignment(MapFont.Alignment.LEFT);
            LocalDateTime now = LocalDateTime.now();
            getLayer(3).draw(MapFont.MINECRAFT, 1, 113, MapColorPalette.getColor(0, 0, 0), now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " UTC");
            getLayer(3).setAlignment(MapFont.Alignment.RIGHT);
            getLayer(3).draw(MapFont.MINECRAFT, 254, 113, MapColorPalette.getColor(0, 0, 0), now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            super.onTick();
        }
    }

}