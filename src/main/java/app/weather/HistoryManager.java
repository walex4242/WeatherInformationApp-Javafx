package app.weather;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HistoryManager {
    private final File file = new File(System.getProperty("user.home"), ".weather_app_history.txt");
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void add(String city) {
        String line = city + " @ " + LocalDateTime.now().format(fmt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            w.write(line + System.lineSeparator());
        } catch (IOException ignored) {}
    }

    public ObservableList<String> load() {
        ObservableList<String> items = FXCollections.observableArrayList();
        if (!file.exists()) return items;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String s;
            while ((s = br.readLine()) != null) {
                if (!s.isBlank()) items.add(s.trim());
            }
        } catch (IOException ignored) {}
        return items;
    }

    public ObservableList<String> getAll() {
        return load();  // Simply return the loaded file contents
    }

    public void clear() {
        if (file.exists()) file.delete();
    }
}
