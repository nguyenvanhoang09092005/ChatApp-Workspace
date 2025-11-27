module org.example.chatappclient {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Java tiêu chuẩn
    requires java.prefs;
    requires java.sql;
    requires java.desktop;

    // JSON
    requires com.google.gson;

    // Apache Commons
    requires org.apache.commons.lang3;
    requires javafx.media;

    // Export toàn bộ package để các module khác hoặc FXML truy cập
    exports org.example.chatappclient.client;
    exports org.example.chatappclient.client.controllers.auth;
    exports org.example.chatappclient.client.controllers.main;
    exports org.example.chatappclient.client.models;
    exports org.example.chatappclient.client.services;
    exports org.example.chatappclient.client.config;
    exports org.example.chatappclient.client.protocol;
    exports org.example.chatappclient.client.utils.validation;
    exports org.example.chatappclient.client.utils.storage;
    exports org.example.chatappclient.client.utils.network;

    // Mở package cho FXML loader (bắt buộc để JavaFX load controller)
    opens org.example.chatappclient.client.controllers.auth to javafx.fxml;
    opens org.example.chatappclient.client.controllers.main to javafx.fxml;
    opens org.example.chatappclient.client.models to javafx.base, com.google.gson;
}
