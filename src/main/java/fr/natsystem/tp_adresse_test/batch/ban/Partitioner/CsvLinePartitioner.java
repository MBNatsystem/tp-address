package fr.natsystem.tp_adresse_test.batch.ban.Partitioner;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class CsvLinePartitioner implements Partitioner {

    private final int totalLines; 

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> map = new HashMap<>(gridSize);

        int targetSize = totalLines / gridSize;
        int start = 1;
        int end = targetSize;
        for(int i = 0; i<gridSize;i++){
            if(i==gridSize-1){
                end = totalLines;
            }
            ExecutionContext context = new ExecutionContext();
            context.putInt("startLine", start);
            context.putInt("endLine", end);

            map.put("Partition_"+i, context);

            start += targetSize;
            end += targetSize; 
        }
        return map;
    }
    
}