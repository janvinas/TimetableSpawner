package io.github.janvinas.timetablespawner;

import net.intelie.omnicron.Cron;

import java.time.LocalDateTime;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class BoardUtils {
    public static TreeMap<LocalDateTime, TrainInformation> fillDepartureBoard(LocalDateTime from, List<String> trainLines, Integer boardLength){
        TreeMap<LocalDateTime, TrainInformation> trainList = new TreeMap<>();

        for(String trainLine : trainLines){
            StringTokenizer lineTokenizer = new StringTokenizer(trainLine, "|");
            TrainInformation trainInformation = new TrainInformation();

            trainInformation.name = lineTokenizer.nextToken();
            trainInformation.time = lineTokenizer.nextToken();
            if(lineTokenizer.hasMoreTokens()) trainInformation.destination = lineTokenizer.nextToken();
            if(lineTokenizer.hasMoreTokens()) trainInformation.platform = lineTokenizer.nextToken();
            if(lineTokenizer.hasMoreTokens()) trainInformation.information = lineTokenizer.nextToken();
            Cron cron = new Cron(trainInformation.time);

            LocalDateTime input = from;
            //put enough trains of every train line so the board will never be empty
            for (int i = 0; i < boardLength; i++) {
                input = cron.next(input);
                trainList.put(input, trainInformation);
            }
        }
        return trainList;
    }
    public static TreeMap<LocalDateTime, TrainInformation> fillDepartureBoard(LocalDateTime from, List<String> trainLines, Integer boardLength, boolean cutAtLength){
        TreeMap<LocalDateTime, TrainInformation> trainList = fillDepartureBoard(from, trainLines, boardLength);
        if(cutAtLength){
            while(trainList.size() > boardLength){
                trainList.remove(trainList.lastKey());
            }
        }
        return trainList;
    }
}
