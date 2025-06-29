package softarrecife.utils;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class Estilos {

    public static Font fuenteNormal = new Font("SansSerif", Font.PLAIN, 14);
    public static Font fuenteTitulo = new Font("SansSerif", Font.BOLD, 18);
    public static Color azulPrimario = new Color(33, 150, 243);
    public static Color grisClaro = new Color(245, 245, 245);
    public static Color grisOscuro = new Color(50, 50, 50);

    public static void estilizarBoton(JButton boton) {
        boton.setBackground(azulPrimario);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setFont(fuenteNormal);
        boton.setBorder(new RoundedBorder(10));
    }

    public static void estilizarLabel(JLabel label) {
        label.setFont(fuenteNormal);
        label.setForeground(grisOscuro);
    }

    public static void estilizarLabelTitulo(JLabel label) {
        label.setFont(fuenteTitulo);
        label.setForeground(new Color(30, 30, 30));
    }

    public static JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        estilizarBoton(btn);
        return btn;
    }

    public static JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(new Color(245, 245, 245));
        btn.setForeground(grisOscuro);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 180, 180), 1, true),
                new EmptyBorder(15, 25, 15, 25)
        ));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);

        // Efecto hover
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(230, 230, 230));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(245, 245, 245));
            }
        });

        return btn;
    }

    public static JButton crearBotonModerno(String texto, Icon icono) {
        JButton boton = new JButton(texto, icono);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setIconTextGap(12);
        boton.setFont(new Font("SansSerif", Font.BOLD, 16));
        boton.setForeground(new Color(40, 40, 40));
        boton.setBackground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setOpaque(true);

        // Borde con efecto 3D
        Border bordeRelieve = BorderFactory.createBevelBorder(BevelBorder.RAISED,
                new Color(255, 255, 255), // High light
                new Color(200, 200, 200), // Shadow
                new Color(160, 160, 160), // Dark shadow
                new Color(230, 230, 230) // Light
        );

        boton.setBorder(BorderFactory.createCompoundBorder(
                bordeRelieve,
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Hover
        boton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(MouseEvent e) {
                boton.setBackground(Color.WHITE);
            }
        });

        return boton;
    }

   public static JButton crearBotonModernoConColor(String texto, Color colorFondo) {
    if (colorFondo == null) {
        colorFondo = new Color(33, 150, 243); // Color azul por defecto
    }

    JButton boton = new JButton(texto);
    boton.setHorizontalAlignment(SwingConstants.LEFT);
    boton.setFont(new Font("SansSerif", Font.BOLD, 15));
    boton.setForeground(Color.BLACK);
    boton.setBackground(colorFondo);
    boton.setFocusPainted(false);
    boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    boton.setOpaque(true);

    Border bordeRelieve = BorderFactory.createBevelBorder(BevelBorder.RAISED,
            new Color(255, 255, 255),
            new Color(200, 200, 200),
            new Color(160, 160, 160),
            new Color(230, 230, 230)
    );

    boton.setBorder(BorderFactory.createCompoundBorder(
            bordeRelieve,
            new EmptyBorder(12, 24, 12, 24)
    ));

    final Color colorFinal = colorFondo;
    boton.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            boton.setBackground(colorFinal.darker());
        }

        public void mouseExited(MouseEvent e) {
            boton.setBackground(colorFinal);
        }
    });

    return boton;
}
   
   public static JButton crearBotonModernoFijo(String texto, Color colorFondo) {
    if (colorFondo == null) {
        colorFondo = new Color(230, 230, 230);
    }

    JButton boton = new JButton(texto);
    boton.setHorizontalAlignment(SwingConstants.LEFT);
    boton.setFont(new Font("SansSerif", Font.BOLD, 15));
    boton.setForeground(Color.BLACK);
    boton.setBackground(colorFondo);
    boton.setFocusPainted(false);
    boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    boton.setOpaque(true);

    Border borde3D = BorderFactory.createBevelBorder(BevelBorder.RAISED,
            new Color(255, 255, 255),
            new Color(200, 200, 200),
            new Color(160, 160, 160),
            new Color(230, 230, 230));

    boton.setBorder(BorderFactory.createCompoundBorder(
            borde3D,
            new EmptyBorder(10, 20, 10, 20)
    ));

    return boton;
}


    public static JButton crearBotonProducto(String texto, ActionListener al) {
        JButton btn = new JButton("<html><center>" + texto + "</center></html>");
        btn.setPreferredSize(new Dimension(140, 70));
        btn.setBackground(Color.WHITE);
        btn.setFont(fuenteNormal);
        btn.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        btn.addActionListener(al);
        return btn;
    }

    public static void estilizarCampo(JTextField campo, String placeholder) {
        campo.setFont(fuenteNormal);
        campo.setText(placeholder);
        campo.setForeground(Color.GRAY);
        campo.setBorder(new RoundedBorder(10));
        campo.setMargin(new Insets(5, 10, 5, 10));

        campo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (campo.getText().equals(placeholder)) {
                    campo.setText("");
                    campo.setForeground(Color.BLACK);
                }
            }

            public void focusLost(FocusEvent e) {
                if (campo.getText().isEmpty()) {
                    campo.setText(placeholder);
                    campo.setForeground(Color.GRAY);
                }
            }
        });
    }

    public static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(28);
        tabla.setFont(fuenteNormal);
        tabla.setGridColor(new Color(230, 230, 230));
        tabla.setSelectionBackground(new Color(180, 205, 255));
        tabla.setSelectionForeground(Color.BLACK);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(azulPrimario);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setReorderingAllowed(false);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color color1 = grisClaro;
            private final Color color2 = new Color(230, 230, 230);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? color1 : color2);
                }
                return c;
            }
        });
    }

    public static JPanel crearPanelConPadding() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        return panel;
    }

    public static JDialog crearDialogo(Component parent, String titulo, int width, int height) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog;

        if (window instanceof Frame) {
            dialog = new JDialog((Frame) window, titulo, true);
        } else if (window instanceof Dialog) {
            dialog = new JDialog((Dialog) window, titulo, true);
        } else {
            dialog = new JDialog((Frame) null, titulo, true);
        }

        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(parent);
        dialog.getContentPane().setBackground(grisClaro);
        return dialog;
    }

    // Borde redondeado reutilizable
    public static class RoundedBorder extends LineBorder {

        private int radius;

        public RoundedBorder(int radius) {
            super(new Color(200, 200, 200), 1, true);
            this.radius = radius;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 2, radius + 5, radius + 2, radius + 5);
        }
    }

 public static Icon cargarIcono(String ruta, int ancho, int alto) {
    try {
        URL url = Estilos.class.getResource(ruta);
        if (url == null) {
            System.err.println("No se encontró el recurso: " + ruta);
            return null;
        }
        ImageIcon icono = new ImageIcon(url);
        Image imagen = icono.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        return new ImageIcon(imagen);
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}
}
