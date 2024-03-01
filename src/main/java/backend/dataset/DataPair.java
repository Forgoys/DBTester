package backend.dataset;

public class DataPair {
    String name;

    String value;

    public DataPair(){}

    public DataPair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public DataPair(String name) {
        this.name = name;
    }
}
