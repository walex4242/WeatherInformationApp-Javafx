package app.weather.model;

public class WeatherData {
    private final String city;
    private final double temperature;
    private final double feelsLike;
    private final int humidity;
    private final double wind;
    private final String condition;
    private final String icon;

    public WeatherData(String city, double temperature, double feelsLike, int humidity, double wind, String condition, String icon) {
        this.city = city;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.wind = wind;
        this.condition = condition;
        this.icon = icon;
    }

    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public double getFeelsLike() { return feelsLike; }
    public int getHumidity() { return humidity; }
    public double getWind() { return wind; }
    public String getCondition() { return condition; }
    public String getIcon() { return icon; }

    // Demo data when API key is missing or for offline screenshot
    public static WeatherData demo() {
        return new WeatherData("São Paulo", 26.3, 27.0, 62, 12.4, "broken clouds", "04d");
    }
}
