package fr.natsystem.tp_adresse_test.batch.geoContour.reader;

import java.io.IOException;

import org.springframework.batch.infrastructure.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class JsonReader<T> implements JsonObjectReader<T> {

    private final JsonMapper mapper;
    private final ObjectReader objectReader;

    private JsonParser parser;
    private boolean finished;

    public JsonReader(JsonMapper mapper, ObjectReader objectReader){
        this.mapper = mapper;
        this.objectReader = objectReader;
    }

    @Override
    public void open(Resource resource)throws Exception {
        parser = mapper.createParser(resource.getInputStream());
        finished = false;
        setPositionOnFeatureArray();
        
    }

    private void setPositionOnFeatureArray() throws Exception{
        JsonToken token;

        while((token = parser.nextToken()) !=null){
            if(token == JsonToken.PROPERTY_NAME && "features".equals(parser.currentName())){
                JsonToken valuToken = parser.nextToken();

                if (valuToken != JsonToken.START_ARRAY){
                    throw new IOException("Properties 'features' is not a JSON array");
                }
                return;
            }   
        }

        throw new IOException("No propertie 'features' found in the geoJSON");
    }

    @SuppressWarnings("unchecked")
    @Override
    public T read() throws Exception {
        
        if(finished){
            return null;
        }

        JsonToken token = parser.nextToken();

        if(token == null || token == JsonToken.END_ARRAY){
            finished = true;
            return null;
        }

        if (token != JsonToken.START_OBJECT){
            throw new IOException("A feature was attempt, encounter token: " + token);
        }

        return (T) objectReader.readValue(parser);
    }

    @Override
    public void jumpToItem(int intemIndex) throws Exception {
        for(int index = 0; index<intemIndex; index ++){
            if (read()==null){
                throw new IOException("Impossible to found the feature "+ intemIndex);
            }
        }
    }

    @Override
    public void close()throws Exception{
        if (parser !=null){
            parser.close();
            parser=null;
        }
        finished=true;
    }
    
}
