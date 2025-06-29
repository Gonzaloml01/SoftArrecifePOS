package softarrecife.utils;

public class ComboItem {
    private String nombre;
    private int id;

    public ComboItem(String nombre, int id) {
        this.nombre = nombre;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
