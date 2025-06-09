module org.calculator.moderncalculator {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;

    opens org.calculator.moderncalculator to javafx.fxml;
    exports org.calculator.moderncalculator;
}