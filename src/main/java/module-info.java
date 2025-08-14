module weather.info.app {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires javafx.swing;
    requires java.net.http;
    requires java.desktop;

    opens app.weather to com.fasterxml.jackson.databind;
    opens app.weather.model to com.fasterxml.jackson.databind;

    exports app.weather;
}
