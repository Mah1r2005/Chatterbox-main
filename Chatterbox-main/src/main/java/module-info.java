module com.chatterbox.lan {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires jdk.httpserver;
    requires jdk.incubator.vector;
    requires jbcrypt;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;


    opens com.chatterbox.lan to javafx.fxml;
    exports com.chatterbox.lan;
    exports com.chatterbox.lan.controllers;
    exports com.chatterbox.lan.database;
    opens com.chatterbox.lan.controllers to javafx.fxml;
}
