package backend.dataset;

import javafx.beans.property.SimpleStringProperty;

public class DisplayResult {
    private SimpleStringProperty name;
    private SimpleStringProperty value;

    public DisplayResult() {}

    public DisplayResult(String name, String value) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
    }

    public String getName() {
        return name.get();
    }

    public String getValue() {
        return value.get();
    }
}
