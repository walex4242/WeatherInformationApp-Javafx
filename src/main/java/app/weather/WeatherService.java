package app.weather;

import app.weather.model.ForecastEntry;
import app.weather.model.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WeatherService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String unitsCurrentMetric = "metric";
    private final String unitsCurrentImperial = "imperial";

    public WeatherService() {
        Properties props = Config.load();
        apiKey = props.getProperty("OPENWEATHER_API_KEY", "").trim();
    }

    public WeatherData fetchWeather(String city, boolean metric) throws Exception {
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing API key. Set OPENWEATHER_API_KEY in src/main/resources/app/weather/config.properties");
        }
        String units = metric ? unitsCurrentMetric : unitsCurrentImperial;
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=%s",
                encode(city), apiKey, units);

        JsonNode json = getJson(url);
        if (json.has("cod") && json.get("cod").asInt() != 200) {
            String msg = json.has("message") ? json.get("message").asText() : "Unknown API error";
            throw new IOException("API error: " + msg);
        }

        double temp = json.get("main").get("temp").asDouble();
        double feels = json.get("main").get("feels_like").asDouble();
        int humidity = json.get("main").get("humidity").asInt();
        double windSpeed = json.get("wind").get("speed").asDouble(); // m/s (metric) or mph (imperial)
        String condition = json.get("weather").get(0).get("description").asText();
        String icon = json.get("weather").get(0).get("icon").asText();

        double windDisplay = metric ? windSpeed * 3.6 : windSpeed; // m/s -> km/h

        return new WeatherData(city, temp, feels, humidity, windDisplay, condition, icon);
    }

    public List<ForecastEntry> fetchForecast(String city, boolean metric, int points) throws Exception {
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing API key. Set OPENWEATHER_API_KEY in src/main/resources/app/weather/config.properties");
        }
        String units = metric ? unitsCurrentMetric : unitsCurrentImperial;
        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=%s",
                encode(city), apiKey, units);

        JsonNode json = getJson(url);
        if (!json.has("list")) {
            throw new IOException("Forecast data not available");
        }

        List<ForecastEntry> out = new ArrayList<>();
        for (int i = 0; i < json.get("list").size() && i < points; i++) {
            JsonNode node = json.get("list").get(i);
            long epoch = node.get("dt").asLong();
            double temp = node.get("main").get("temp").asDouble();
            double windSpeed = node.get("wind").get("speed").asDouble();
            String cond = node.get("weather").get(0).get("main").asText();
            String icon = node.get("weather").get(0).get("icon").asText();
            double windDisplay = metric ? windSpeed * 3.6 : windSpeed;
            out.add(new ForecastEntry(epoch, temp, windDisplay, cond, icon));
        }
        return out;
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new IOException("HTTP error " + resp.statusCode());
        return mapper.readTree(resp.body());
    }

    private String encode(String s) {
        return s.replace(" ", "%20");
    }
}
