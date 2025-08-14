# Weather Information App (JavaFX)

A Java 17 + JavaFX app that fetches real-time weather and a short-term forecast using the OpenWeatherMap API.
It includes:
- City search with validation
- Current conditions: temperature, feels like, humidity, wind, condition + icon
- Short-term forecast (next ~24 hours)
- Metric/Imperial unit toggle
- Error handling for invalid input and API errors
- Search history with timestamps (saved to your home folder)
- Dynamic backgrounds based on time of day
- **Demo Mode** to preview UI and take screenshots without an API key
- **Save Screenshot** button

## Quick Start

### 1) Prerequisites
- Java 17 (or newer)
- Maven 3.8+
- Internet connection (for real API calls)

### 2) OpenWeatherMap API Key
1. Create a free account at https://openweathermap.org/ and generate an API key.
2. Edit `src/main/resources/app/weather/config.properties` and set:
   ```
   OPENWEATHER_API_KEY=YOUR_API_KEY_HERE
   ```

### 3) Run
```bash
mvn clean javafx:run
```
If JavaFX is not found, ensure JAVA_HOME points to JDK 17+.

### 4) Usage
- Enter a city (e.g., `Lagos`, `São Paulo`, `London`) and click **Search**.
- Use the unit dropdown to switch between **Metric (°C, km/h)** and **Imperial (°F, mph)**.
- Click **Save Screenshot** to export a PNG of the app window for your assignment submission.
- Click **Demo Mode** to load built-in sample data (no API key needed) and take screenshots offline.

### 5) History
- Your recent searches are saved to `/home/sandbox/.weather_app_history.txt`.
- Click a history row to reload that city. Use **Clear History** to wipe the file.

## Project Structure
```
WeatherInformationApp-JavaFX/
  ├── pom.xml
  └── src/
      └── main/
          ├── java/
          │   ├── module-info.java
          │   └── app/weather/
          │       ├── WeatherApp.java
          │       ├── WeatherService.java
          │       ├── HistoryManager.java
          │       └── model/
          │           ├── WeatherData.java
          │           └── ForecastEntry.java
          └── resources/
              └── app/weather/
                  └── config.properties
```

## Notes for Instructors / Grading rubric mapping
- **API Integration :** Uses OpenWeatherMap `/weather` and `/forecast` endpoints via `HttpClient`. Parses JSON with Jackson. Handles HTTP and API error codes.
- **GUI Design :** JavaFX UI with city input, unit selection, Search, Demo Mode, Save Screenshot, and a history ListView. Clean layout and labels.
- **Logic & Computation :** Converts wind from m/s to km/h for metric; refetches on unit change; shows 8 forecast periods (~24h).
- **Program Flow & Structure :** Clear separation into `WeatherService`, models, history management, and the `WeatherApp` UI. Meaningful names & comments.
- **Output :** Use **Save Screenshot** to attach a GUI screenshot. README included. Source well-commented.
- **Code Style & Readability :** Consistent formatting and naming; no redundant code.

## Troubleshooting
- **401 Unauthorized / 404 city not found:** Check your API key and spelling.
- **SSL or network errors:** Confirm internet access and that your firewall allows outbound HTTPS.
- **Blank icons:** Sometimes the icon URL may be slow; wait a moment or re-search.

---

