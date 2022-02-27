package dk.sdu.subscription.ServiceLayer.SecretService;

import dk.sdu.subscription.Interfaces.ISecretService;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SecretHandler implements ISecretService {

    private Map<String, Object> secretCache;

    public SecretHandler(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()){
            throw new FileNotFoundException("Unable to find file: " + filePath);
        }
        String contents = Files.readString(Path.of(file.getAbsolutePath()));
        JSONObject config = new JSONObject(contents);
        this.secretCache = new HashMap<>();
        for (Iterator<String> it = config.keys(); it.hasNext(); ) {
            String key = it.next();
            if(config.get(key) instanceof String){
                secretCache.put(key, config.get(key));
            }else if(config.get(key) instanceof Integer){
                secretCache.put(key, config.get(key));
            }else if(config.get(key) instanceof Boolean){
                secretCache.put(key, config.get(key));
            }
        }
    }

    public boolean hasSecret(String key){
        return this.secretCache.containsKey(key);
    }

    public Object getSecret(String key){
        return this.secretCache.get(key);
    }

    public String getStringSecret(String key){
        return (String) getSecret(key);
    }

    public Integer getIntSecret(String key){
        return (Integer) getSecret(key);
    }

    public Boolean getBooleanSecret(String key){
        return (Boolean) getSecret(key);
    }
}
