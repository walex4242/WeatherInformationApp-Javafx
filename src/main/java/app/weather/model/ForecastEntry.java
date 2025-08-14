package app.weather.model;

import java.util.ArrayList;
import java.util.List;

public class ForecastEntry {
    private final long epoch;
    private final double temp;
    private final double wind;
    private final String condition;
    private final String icon;

    public ForecastEntry(long epoch, double temp, double wind, String condition, String icon) {
        this.epoch = epoch;
        this.temp = temp;
        this.wind = wind;
        this.condition = condition;
        this.icon = icon;
    }

    public long getEpoch() { return epoch; }
    public double getTemp() { return temp; }
    public double getWind() { return wind; }
    public String getCondition() { return condition; }
    public String getIcon() { return icon; }

    public static List<ForecastEntry> demoList() {
        long now = System.currentTimeMillis() / 1000;
        List<ForecastEntry> list = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            list.add(new ForecastEntry(now + i * 3 * 3600L, 24 + Math.sin(i)*2, 10 + i, i % 2 == 0 ? "Clouds" : "Clear", i % 2 == 0 ? "03d" : "01d"));
        }
        return list;
    }
}
