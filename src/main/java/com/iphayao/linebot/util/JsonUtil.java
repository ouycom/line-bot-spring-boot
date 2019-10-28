package com.iphayao.linebot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonUtil {

    public Map<String, Object> json2Map(String json) throws IOException {
        HashMap<String, Object> result = new ObjectMapper().readValue(json, HashMap.class);
        return result;
    }

    public String map2json(Map<String, Object> map) throws JsonProcessingException {
        return object2json(map, false);
    }

    public String map2jsonPretty(Map<String, Object> map) throws JsonProcessingException {
        return object2json(map, true);
    }

    public String list2json(List list) throws JsonProcessingException {
        return object2json(list, false);
    }

    public String list2jsonPretty(List list) throws JsonProcessingException {
        return object2json(list, true);
    }

    public String object2json(Object model) throws JsonProcessingException {
        return object2json(model, false);
    }

    public String object2jsonPretty(Object model) throws JsonProcessingException {
        return object2json(model, true);
    }

    private String object2json(Object model, boolean prettyPrint) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        if(prettyPrint) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
        }else{
            return mapper.writeValueAsString(model);
        }
    }

    public Object map2Object(Map<String, Object> map, Class c) throws IOException {
        String str = map2json(map);
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(str, c);
    }

//    public Object list2Object(List<Map<String, Object>> list, Object obj) throws IOException {
//        String str = list2json(list);
//        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(str, obj.getClass());
//    }
}
