module com.savvy.stocksavvyyloglog {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires java.net.http;
    requires com.google.gson;

    exports com.savvy.stocksavvyyloglog.controller;
    opens   com.savvy.stocksavvyyloglog.controller to javafx.fxml;

    exports com.savvy.stocksavvyyloglog.model;
    opens   com.savvy.stocksavvyyloglog.model to javafx.fxml;

    exports com.savvy.stocksavvyyloglog.view;
    opens   com.savvy.stocksavvyyloglog.view to javafx.fxml;

    exports com.savvy.stocksavvyyloglog.dialog;
    opens   com.savvy.stocksavvyyloglog.dialog to javafx.fxml;

    exports com.savvy.stocksavvyyloglog.app;
    opens   com.savvy.stocksavvyyloglog.app to javafx.fxml;

    exports com.savvy.stocksavvyyloglog.util;
    opens   com.savvy.stocksavvyyloglog.util to javafx.fxml;
}