package ro.uaic.info.gitprov.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProvOVizService {

    public Map<String, String> getFormParameters(String content) {
        Map<String, String> result = new HashMap<>();
        result.put("data", content);
        result.put("format", "turtle");

        return result;
    }
}
