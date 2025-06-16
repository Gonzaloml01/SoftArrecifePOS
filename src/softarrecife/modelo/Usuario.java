package softarrecife.modelo;

public class Usuario {
    private int id;
    private String nombre;
    private String usuario;
    private String contrasena;
    private String rol;

    public Usuario(int id, String nombre, String usuario, String contrasena, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUsuario() { return usuario; }
    public String getContrasena() { return contrasena; }
    public String getRol() { return rol; }
}
