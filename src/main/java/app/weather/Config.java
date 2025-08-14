package app.weather;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    public static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getResourceAsStream("/app/weather/config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) { }
        return props;
    }
}
