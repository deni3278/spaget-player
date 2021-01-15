module spaget {
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;
    requires jaudiotagger;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens spaget to javafx.fxml;
    exports spaget;
}