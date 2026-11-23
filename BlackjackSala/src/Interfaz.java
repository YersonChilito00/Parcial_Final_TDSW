import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Interfaz gráfica sencilla (Swing) para la sala de blackjack.
 *
 * No cambia las reglas ni las estructuras: usa las mismas clases del proyecto
 * (Mesa, Sala, Grafo...). Todo lo que el programa imprime en consola aparece
 * en el "registro" de la ventana.
 *
 * Pestañas:
 *   1. Partida: mesa visual, apuestas, botones Pedir / Plantarse y botones para
 *      ver cada estructura (asientos, tabla hash, descarte, historial, ranking).
 *   2. Sala (grafo): dibujo del grafo con Dijkstra, Prim y Kruskal.
 *   3. Corrida de verificación: la salida completa de consola en un cuadro de texto.
 *
 * Ejecutar: java Interfaz   (o  java Main gui)
 */
public class Interfaz extends JFrame {

    private static final Color FIELTRO = new Color(0x1B6B45);
    private static final Color FIELTRO_OSCURO = new Color(0x0F4A2E);
    private static final Color DORADO = new Color(0xF2C94C);

    public static void main(String[] args) {
        // Lo que se imprime con System.out va a la ventana (con UTF-8).
        try {
            System.setOut(new PrintStream(new Salida(), true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // si falla se queda la salida normal
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // look and feel por defecto
                }
                new Interfaz().setVisible(true);
            }
        });
    }

    public Interfaz() {
        super("Sala de blackjack — Estructuras de Datos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        PartidaPanel partida = new PartidaPanel();
        Salida.defecto = partida.sumidero();

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Partida", partida);
        pestanas.addTab("Sala (grafo)", new SalaPanel());
        pestanas.addTab("Corrida de verificación", new CorridaPanel(partida));
        setContentPane(pestanas);

        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
    }

    // ==================================================================
    // Salida de consola -> ventana
    // ==================================================================

    /**
     * Recibe los bytes de System.out y entrega cada línea completa a un
     * "destino". Cada hilo puede tener su propio destino (para capturar la
     * salida de una acción sin mezclarla con la de la ronda que está jugando).
     */
    static class Salida extends OutputStream {
        static final ThreadLocal<Consumer<String>> especial = new ThreadLocal<Consumer<String>>();
        static volatile Consumer<String> defecto = new Consumer<String>() {
            public void accept(String s) {
            }
        };

        private final ThreadLocal<ByteArrayOutputStream> buffer = new ThreadLocal<ByteArrayOutputStream>() {
            @Override
            protected ByteArrayOutputStream initialValue() {
                return new ByteArrayOutputStream();
            }
        };

        @Override
        public void write(int b) {
            ByteArrayOutputStream bb = buffer.get();
            bb.write(b);
            if (b == '\n') {
                String linea;
                try {
                    linea = bb.toString("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    linea = bb.toString();
                }
                bb.reset();
                Consumer<String> destino = especial.get();
                if (destino == null) {
                    destino = defecto;
                }
                destino.accept(linea);
            }
        }
    }

    /** Ejecuta algo y devuelve todo lo que imprimió. */
    static String capturar(Runnable accion) {
        final StringBuilder sb = new StringBuilder();
        Salida.especial.set(new Consumer<String>() {
            public void accept(String s) {
                sb.append(s);
            }
        });
        try {
            accion.run();
        } finally {
            Salida.especial.remove();
        }
        return sb.toString();
    }

    private static JTextArea areaDeTexto() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(false);
        area.setMargin(new java.awt.Insets(6, 8, 6, 8));
        return area;
    }

    private static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFocusable(false);
        return b;
    }

    // ==================================================================
    // Una carta dibujada
    // ==================================================================

    static class CartaComp extends JComponent {
        private final Carta carta;
        private final boolean oculta;

        CartaComp(Carta carta, boolean oculta) {
            this.carta = carta;
            this.oculta = oculta;
            setPreferredSize(new Dimension(58, 82));
        }

        private static String abreviar(String rango) {
            if (rango.equals("As")) {
                return "A";
            }
            if (rango.equals("Jota")) {
                return "J";
            }
            if (rango.equals("Reina")) {
                return "Q";
            }
            if (rango.equals("Rey")) {
                return "K";
            }
            return rango;
        }

        private static String simbolo(String palo) {
            switch (palo.charAt(0)) {
                case 'p':
                    return "\u2660";
                case 'c':
                    return "\u2665";
                case 'd':
                    return "\u2666";
                default:
                    return "\u2663";
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 3;
            int h = getHeight() - 3;
            RoundRectangle2D forma = new RoundRectangle2D.Float(1, 1, w, h, 12, 12);
            if (oculta) {
                g.setPaint(new GradientPaint(0, 0, new Color(0x2F4CA8), w, h, new Color(0x1B2D6E)));
                g.fill(forma);
                g.setColor(new Color(255, 255, 255, 90));
                g.setStroke(new BasicStroke(1f));
                for (int i = -h; i < w; i += 9) {
                    g.drawLine(i, h, i + h, 0);
                }
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f));
                g.draw(forma);
            } else {
                g.setColor(Color.WHITE);
                g.fill(forma);
                g.setColor(new Color(0xB8B8B8));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(forma);
                boolean roja = carta.getPalo().charAt(0) == 'c' || carta.getPalo().charAt(0) == 'd';
                g.setColor(roja ? new Color(0xC62828) : new Color(0x212121));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                g.drawString(abreviar(carta.getRango()), 7, 21);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
                g.drawString(simbolo(carta.getPalo()), 7, 38);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
                java.awt.FontMetrics fm = g.getFontMetrics();
                String s = simbolo(carta.getPalo());
                g.drawString(s, (w - fm.stringWidth(s)) / 2 + 3, h - 14);
            }
            g.dispose();
        }
    }

    /** Panel con el fondo verde de la mesa. */
    static class Fieltro extends JPanel {
        Fieltro() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(new GradientPaint(0, 0, FIELTRO, 0, getHeight(), FIELTRO_OSCURO));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    // ==================================================================
    // Pestaña 1: la partida
    // ==================================================================

    static class PartidaPanel extends JPanel {

        private static final String[][] DATOS = {
            {"J01", "Ana", "500"}, {"J02", "Luis", "400"}, {"J06", "Marta", "300"}
        };

        /** Un asiento: nombre, fichas, apuesta, cartas, puntaje y estado. */
        private class AsientoPanel extends JPanel {
            final JLabel lblNombre = new JLabel();
            final JLabel lblFichas = new JLabel();
            final JSpinner spApuesta = new JSpinner(new SpinnerNumberModel(0, 0, 0, 10));
            final JPanel cartas = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
            final JLabel lblPuntaje = new JLabel(" ");
            final JLabel lblEstado = new JLabel(" ");

            AsientoPanel() {
                setLayout(new BorderLayout(0, 6));
                setOpaque(false);
                setBorder(bordeNormal());

                JPanel arriba = new JPanel(new GridLayout(3, 1, 0, 2));
                arriba.setOpaque(false);
                lblNombre.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                lblNombre.setForeground(Color.WHITE);
                lblFichas.setForeground(DORADO);
                lblFichas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                JPanel filaApuesta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                filaApuesta.setOpaque(false);
                JLabel et = new JLabel("Apuesta:  ");
                et.setForeground(Color.WHITE);
                filaApuesta.add(et);
                spApuesta.setPreferredSize(new Dimension(80, 24));
                filaApuesta.add(spApuesta);
                arriba.add(lblNombre);
                arriba.add(lblFichas);
                arriba.add(filaApuesta);
                add(arriba, BorderLayout.NORTH);

                cartas.setOpaque(false);
                add(cartas, BorderLayout.CENTER);

                JPanel abajo = new JPanel(new GridLayout(2, 1));
                abajo.setOpaque(false);
                lblPuntaje.setForeground(Color.WHITE);
                lblPuntaje.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                lblEstado.setForeground(Color.WHITE);
                abajo.add(lblPuntaje);
                abajo.add(lblEstado);
                add(abajo, BorderLayout.SOUTH);
            }
        }

        private static Border bordeNormal() {
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 255, 255, 70), 2, true),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }

        private static Border bordeTurno() {
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DORADO, 3, true),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9));
        }

        private Mesa mesa;
        private Jugador[] jug;
        private final boolean[] juega = new boolean[DATOS.length];
        private final int[] fichasAntes = new int[DATOS.length];
        private boolean fija;
        private volatile boolean rondaEnCurso;
        private volatile Jugador turnoActual;

        private final Object cerrojo = new Object();
        private Boolean respuesta;

        private final AsientoPanel[] asientos = new AsientoPanel[DATOS.length];
        private final JPanel cartasCrupier = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
        private final JLabel lblCrupier = new JLabel("Crupier");
        private final JLabel lblPuntajeCrupier = new JLabel(" ");
        private final JLabel lblZapato = new JLabel(" ");
        private final JLabel lblTurno = new JLabel(" ");
        private final JLabel lblAyuda = new JLabel(" ");
        private final JComboBox<String> cboMazo = new JComboBox<String>(
                new String[]{"Cartas fijas (corrida del enunciado)", "Mazo barajado (modo libre)"});
        private final JButton btnNueva = boton("Nueva mesa");
        private final JButton btnRepartir = boton("Repartir ronda");
        private final JButton btnPedir = boton("Pedir carta");
        private final JButton btnPlantar = boton("Plantarse");
        private final JButton[] btnEstructuras;
        private final JTextArea log = areaDeTexto();

        PartidaPanel() {
            setLayout(new BorderLayout());

            // --- barra superior
            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            barra.add(new JLabel("Zapato:"));
            barra.add(cboMazo);
            barra.add(btnNueva);
            barra.add(lblAyuda);
            add(barra, BorderLayout.NORTH);

            // --- la mesa
            Fieltro mesaPanel = new Fieltro();
            mesaPanel.setLayout(new BorderLayout(0, 10));
            mesaPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JPanel crupier = new JPanel(new BorderLayout());
            crupier.setOpaque(false);
            JPanel titulo = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
            titulo.setOpaque(false);
            lblCrupier.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            lblCrupier.setForeground(Color.WHITE);
            lblPuntajeCrupier.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            lblPuntajeCrupier.setForeground(Color.WHITE);
            lblZapato.setForeground(DORADO);
            titulo.add(lblCrupier);
            titulo.add(lblPuntajeCrupier);
            titulo.add(lblZapato);
            crupier.add(titulo, BorderLayout.NORTH);
            cartasCrupier.setOpaque(false);
            cartasCrupier.setPreferredSize(new Dimension(100, 96));
            crupier.add(cartasCrupier, BorderLayout.CENTER);
            mesaPanel.add(crupier, BorderLayout.NORTH);

            JPanel fila = new JPanel(new GridLayout(1, DATOS.length, 12, 0));
            fila.setOpaque(false);
            for (int i = 0; i < DATOS.length; i++) {
                asientos[i] = new AsientoPanel();
                fila.add(asientos[i]);
            }
            mesaPanel.add(fila, BorderLayout.CENTER);

            lblTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            lblTurno.setForeground(DORADO);
            lblTurno.setHorizontalAlignment(JLabel.CENTER);
            mesaPanel.add(lblTurno, BorderLayout.SOUTH);

            // --- controles de juego y de estructuras
            JPanel juego = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
            btnRepartir.setFont(btnRepartir.getFont().deriveFont(Font.BOLD, 14f));
            btnPedir.setFont(btnPedir.getFont().deriveFont(Font.BOLD, 14f));
            btnPlantar.setFont(btnPlantar.getFont().deriveFont(Font.BOLD, 14f));
            juego.add(btnRepartir);
            juego.add(btnPedir);
            juego.add(btnPlantar);

            JPanel estructuras = new JPanel(new GridLayout(2, 5, 6, 4));
            estructuras.setBorder(BorderFactory.createTitledBorder("Ver las estructuras"));
            btnEstructuras = new JButton[]{
                boton("Asientos ↔"), boton("Tabla hash"), boton("Manos ↔"), boton("Descarte (pila)"),
                boton("Historial (pila)"), boton("Deshacer"), boton("Ranking (árbol)"),
                boton("Árbol de decisión"), boton("Zapato vacío")
            };
            for (int i = 0; i < btnEstructuras.length; i++) {
                estructuras.add(btnEstructuras[i]);
            }

            JPanel sur = new JPanel(new BorderLayout());
            sur.add(juego, BorderLayout.NORTH);
            sur.add(estructuras, BorderLayout.CENTER);

            JPanel izquierda = new JPanel(new BorderLayout());
            izquierda.add(mesaPanel, BorderLayout.CENTER);
            izquierda.add(sur, BorderLayout.SOUTH);

            // --- registro
            log.setLineWrap(true);
            log.setWrapStyleWord(true);
            JScrollPane scroll = new JScrollPane(log);
            scroll.setBorder(BorderFactory.createTitledBorder("Registro (lo que imprime el programa)"));
            scroll.setPreferredSize(new Dimension(430, 100));

            add(izquierda, BorderLayout.CENTER);
            add(scroll, BorderLayout.EAST);

            conectarBotones();
            nuevaMesa();
        }

        /** Destino de lo que imprime el programa: el registro de esta pestaña. */
        Consumer<String> sumidero() {
            return new Consumer<String>() {
                public void accept(final String linea) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            log.append(linea);
                            log.setCaretPosition(log.getDocument().getLength());
                            refrescar();
                        }
                    });
                }
            };
        }

        boolean rondaEnCurso() {
            return rondaEnCurso;
        }

        // ------------------------------------------------------------
        // Botones
        // ------------------------------------------------------------

        private void conectarBotones() {
            btnNueva.addActionListener(e -> nuevaMesa());
            btnRepartir.addActionListener(e -> repartir());
            btnPedir.addActionListener(e -> responder(true));
            btnPlantar.addActionListener(e -> responder(false));

            btnEstructuras[0].addActionListener(e -> {
                titulo("Asientos (lista doble)");
                mesa.imprimirAsientos();
            });
            btnEstructuras[1].addActionListener(e -> {
                titulo("Tabla hash");
                mesa.getTabla().imprimir();
                for (int i = 0; i < jug.length; i++) {
                    mesa.getTabla().buscarConTraza(jug[i].id);
                }
                mesa.getTabla().buscarConTraza("J99");
            });
            btnEstructuras[2].addActionListener(e -> {
                titulo("Manos (lista doble, en los dos sentidos)");
                for (int i = 0; i < jug.length; i++) {
                    if (jug[i].mano.numCartas() > 0) {
                        mesa.imprimirMano(jug[i].id);
                    }
                }
            });
            btnEstructuras[3].addActionListener(e -> {
                titulo("Descarte (pila)");
                mesa.imprimirDescarte();
            });
            btnEstructuras[4].addActionListener(e -> {
                titulo("Historial (pila)");
                mesa.imprimirHistorial();
            });
            btnEstructuras[5].addActionListener(e -> {
                titulo("Deshacer (retira la cima del historial)");
                mesa.deshacer();
            });
            btnEstructuras[6].addActionListener(e -> {
                titulo("Ranking (árbol binario de búsqueda)");
                if (fija) {
                    System.out.println("Se insertan las fichas finales en este orden: Marta, Ana, Luis.");
                    mesa.armarRanking("J06", "J01", "J02");
                } else {
                    System.out.println("Se insertan las fichas finales en el orden de los asientos.");
                    mesa.armarRankingPorAsientos();
                }
                mesa.imprimirRanking(mesa.fichasDe(fija ? "J02" : "J01"));
            });
            btnEstructuras[7].addActionListener(e -> {
                titulo("Árbol de decisión");
                mesa.getArbolDecision().imprimir();
            });
            btnEstructuras[8].addActionListener(e -> {
                titulo("Caso de borde: pedir carta con el zapato vacío");
                mesa.probarZapatoVacio(mesa.getTabla().buscar("J02"));
            });
        }

        private void titulo(String t) {
            System.out.println();
            System.out.println("--- " + t + " ---");
        }

        // ------------------------------------------------------------
        // Mesa nueva
        // ------------------------------------------------------------

        private void nuevaMesa() {
            fija = cboMazo.getSelectedIndex() == 0;
            Cola<Carta> zapato = fija ? new Baraja().zapatoDeCorrida() : new Baraja().zapatoBarajado(new Random());
            mesa = new Mesa(zapato, null);
            mesa.setDecisor(new Mesa.Decisor() {
                public boolean pide(Jugador j) {
                    return decidir(j);
                }
            });
            jug = new Jugador[DATOS.length];
            for (int i = 0; i < DATOS.length; i++) {
                jug[i] = new Jugador(DATOS[i][0], DATOS[i][1], Integer.parseInt(DATOS[i][2]));
                mesa.sentar(jug[i]);
                juega[i] = false;
                fichasAntes[i] = jug[i].fichas;
                SpinnerNumberModel m = (SpinnerNumberModel) asientos[i].spApuesta.getModel();
                m.setMaximum(jug[i].fichas);
                m.setValue(0);
            }
            turnoActual = null;
            rondaEnCurso = false;
            log.setText("");
            System.out.println("Mesa nueva (" + (fija ? "cartas fijas" : "mazo barajado") + ").");
            mesa.imprimirAsientos();
            lblTurno.setText("Pon una apuesta (0 = no juega) y pulsa «Repartir ronda».");
            lblAyuda.setText(fija
                    ? "<html><i>Guía: ronda 1 → Ana 100, Luis 50 (Luis pide). Ronda 2 → Marta 80, Ana 100 (Marta se planta; Ana pide y se planta).</i></html>"
                    : " ");
            setControles(false);
            refrescar();
        }

        // ------------------------------------------------------------
        // La ronda corre en otro hilo; cuando toca decidir, espera el clic
        // ------------------------------------------------------------

        private void repartir() {
            if (rondaEnCurso) {
                return;
            }
            int apuestan = 0;
            for (int i = 0; i < jug.length; i++) {
                int monto = ((Integer) asientos[i].spApuesta.getValue()).intValue();
                if (monto > 0) {
                    mesa.apostar(jug[i].id, monto);
                }
                juega[i] = jug[i].apuesta > 0;
                fichasAntes[i] = jug[i].fichas;
                if (juega[i]) {
                    apuestan++;
                }
            }
            if (apuestan == 0) {
                lblTurno.setText("Nadie ha apostado: sube la apuesta de alguien.");
                return;
            }
            rondaEnCurso = true;
            setControles(true);
            lblTurno.setText("Repartiendo...");
            Thread hilo = new Thread(new Runnable() {
                public void run() {
                    try {
                        mesa.jugarRonda();
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                terminarRonda();
                            }
                        });
                    }
                }
            }, "ronda");
            hilo.setDaemon(true);
            hilo.start();
        }

        private void terminarRonda() {
            rondaEnCurso = false;
            turnoActual = null;
            for (int i = 0; i < jug.length; i++) {
                SpinnerNumberModel m = (SpinnerNumberModel) asientos[i].spApuesta.getModel();
                m.setMaximum(Math.max(0, jug[i].fichas));
                if (((Integer) m.getValue()).intValue() > jug[i].fichas) {
                    m.setValue(Math.max(0, jug[i].fichas));
                }
            }
            lblTurno.setText("Ronda terminada. Ajusta las apuestas y reparte otra vez.");
            setControles(false);
            refrescar();
        }

        /** Se llama desde el hilo de la ronda: espera hasta que la persona pulse un botón. */
        private boolean decidir(final Jugador j) {
            synchronized (cerrojo) {
                turnoActual = j;
                respuesta = null;
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    btnPedir.setEnabled(true);
                    btnPlantar.setEnabled(true);
                    lblTurno.setText("Turno de " + j.nombre + " (" + j.mano.getPuntaje() + " puntos): ¿pides carta?");
                    refrescar();
                }
            });
            synchronized (cerrojo) {
                while (respuesta == null) {
                    try {
                        cerrojo.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        respuesta = Boolean.FALSE;
                    }
                }
                turnoActual = null;
                return respuesta.booleanValue();
            }
        }

        private void responder(boolean pide) {
            btnPedir.setEnabled(false);
            btnPlantar.setEnabled(false);
            lblTurno.setText(" ");
            synchronized (cerrojo) {
                respuesta = Boolean.valueOf(pide);
                cerrojo.notifyAll();
            }
        }

        private void setControles(boolean enRonda) {
            btnRepartir.setEnabled(!enRonda);
            btnNueva.setEnabled(!enRonda);
            cboMazo.setEnabled(!enRonda);
            btnPedir.setEnabled(false);
            btnPlantar.setEnabled(false);
            for (int i = 0; i < asientos.length; i++) {
                asientos[i].spApuesta.setEnabled(!enRonda && jug[i].fichas > 0);
            }
            for (int i = 0; i < btnEstructuras.length; i++) {
                btnEstructuras[i].setEnabled(!enRonda);
            }
        }

        // ------------------------------------------------------------
        // Dibujar el estado actual de la mesa
        // ------------------------------------------------------------

        private void refrescar() {
            if (mesa == null) {
                return;
            }
            Jugador turno = turnoActual;
            for (int i = 0; i < jug.length; i++) {
                Jugador j = jug[i];
                AsientoPanel a = asientos[i];
                a.lblNombre.setText(j.nombre + "  (" + j.id + ")");
                a.lblFichas.setText("Fichas: " + j.fichas);
                a.cartas.removeAll();

                boolean participa = juega[i] && j.mano.numCartas() > 0;
                if (participa) {
                    Carta[] cs = j.mano.comoArreglo();
                    for (int k = 0; k < cs.length; k++) {
                        a.cartas.add(new CartaComp(cs[k], false));
                    }
                    a.lblPuntaje.setText("Puntaje: " + j.mano.getPuntaje()
                            + (juega[i] && rondaEnCurso ? "   (apuesta " + j.apuesta + ")" : ""));
                } else {
                    a.lblPuntaje.setText(" ");
                }

                String estado;
                if (!participa) {
                    estado = rondaEnCurso || mesaConRondas() ? "No juega esta ronda" : " ";
                } else if (rondaEnCurso) {
                    if (j.paso) {
                        estado = "Se pasó";
                    } else if (j.blackjack) {
                        estado = "¡Blackjack!";
                    } else if (j == turno) {
                        estado = "Su turno";
                    } else {
                        estado = "Esperando";
                    }
                } else {
                    int delta = j.fichas - fichasAntes[i];
                    if (delta > 0) {
                        estado = "Ganó " + delta;
                    } else if (delta < 0) {
                        estado = "Perdió " + (-delta);
                    } else {
                        estado = "Empate: recupera la apuesta";
                    }
                }
                a.lblEstado.setText(estado);
                a.setBorder(j == turno ? bordeTurno() : bordeNormal());
                a.cartas.revalidate();
                a.cartas.repaint();
            }

            // crupier
            cartasCrupier.removeAll();
            Carta[] cc = mesa.getManoCrupier().comoArreglo();
            boolean ver = mesa.isCrupierRevelado() || !rondaEnCurso;
            for (int k = 0; k < cc.length; k++) {
                cartasCrupier.add(new CartaComp(cc[k], k == 1 && !ver));
            }
            if (cc.length == 0) {
                lblPuntajeCrupier.setText(" ");
            } else if (ver) {
                lblPuntajeCrupier.setText("Puntaje: " + mesa.getManoCrupier().getPuntaje());
            } else {
                lblPuntajeCrupier.setText("Puntaje: ?");
            }
            lblZapato.setText("Zapato: " + mesa.cartasEnZapato() + " cartas  |  Descarte: " + mesa.cartasEnDescarte());
            cartasCrupier.revalidate();
            cartasCrupier.repaint();
        }

        private boolean mesaConRondas() {
            return mesa.cartasEnDescarte() > 0;
        }
    }

    // ==================================================================
    // Pestaña 2: la sala (grafo)
    // ==================================================================

    static class SalaPanel extends JPanel {

        // posiciones relativas de R, T1, T2, T3 y T4
        private static final double[][] POS = {
            {0.10, 0.50}, {0.38, 0.18}, {0.38, 0.82}, {0.80, 0.18}, {0.80, 0.82}
        };
        private static final int NORMAL = 0;
        private static final int CAMINO = 1;
        private static final int ACEPTADA = 2;
        private static final int RECHAZADA = 3;

        private final Sala sala = new Sala();
        private final Grafo grafo = sala.getGrafo();
        private final int[][] estilo = new int[grafo.tamano()][grafo.tamano()];
        private int destino = -1;
        private final JTextArea texto = areaDeTexto();
        private final JCheckBox chkLlena = new JCheckBox("T1 llena");
        private final JTextField txtMesa = new JTextField("T9", 5);
        private final JComponent lienzo;

        SalaPanel() {
            setLayout(new BorderLayout());

            lienzo = new JComponent() {
                @Override
                protected void paintComponent(Graphics g0) {
                    pintar((Graphics2D) g0.create(), getWidth(), getHeight());
                }
            };
            lienzo.setPreferredSize(new Dimension(640, 420));

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            JButton bDij = boton("Dijkstra: mesa libre más cercana");
            JButton bPrim = boton("Prim");
            JButton bKru = boton("Kruskal");
            JButton bIr = boton("Camino a:");
            barra.add(chkLlena);
            barra.add(bDij);
            barra.add(bPrim);
            barra.add(bKru);
            barra.add(bIr);
            barra.add(txtMesa);
            add(barra, BorderLayout.NORTH);

            add(lienzo, BorderLayout.CENTER);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            JScrollPane sp = new JScrollPane(texto);
            sp.setPreferredSize(new Dimension(430, 100));
            sp.setBorder(BorderFactory.createTitledBorder("Resultado"));
            add(sp, BorderLayout.EAST);

            bDij.addActionListener(e -> dijkstra());
            bPrim.addActionListener(e -> conexion(true));
            bKru.addActionListener(e -> conexion(false));
            bIr.addActionListener(e -> caminoA());
            chkLlena.addActionListener(e -> {
                sala.marcarLlena("T1", chkLlena.isSelected());
                dijkstra();
            });

            dijkstra();
        }

        private void limpiar() {
            for (int i = 0; i < estilo.length; i++) {
                for (int j = 0; j < estilo.length; j++) {
                    estilo[i][j] = NORMAL;
                }
            }
            destino = -1;
        }

        private void marcar(int u, int v, int tipo) {
            estilo[u][v] = tipo;
            estilo[v][u] = tipo;
        }

        private void marcarCamino(int mesaDestino) {
            Grafo.CaminoMinimo cm = grafo.dijkstra(grafo.indiceDe("R"));
            if (cm.dist[mesaDestino] >= Grafo.INFINITO) {
                return;
            }
            destino = mesaDestino;
            for (int v = mesaDestino; cm.previo[v] != -1; v = cm.previo[v]) {
                marcar(v, cm.previo[v], CAMINO);
            }
        }

        private void dijkstra() {
            sala.marcarLlena("T1", chkLlena.isSelected());
            limpiar();
            final String[] elegida = new String[1];
            String salida = capturar(new Runnable() {
                public void run() {
                    elegida[0] = sala.buscarMesaLibre();
                }
            });
            if (elegida[0] != null) {
                marcarCamino(grafo.indiceDe(elegida[0]));
            }
            texto.setText(salida);
            lienzo.repaint();
        }

        private void caminoA() {
            final String mesa = txtMesa.getText().trim();
            limpiar();
            texto.setText(capturar(new Runnable() {
                public void run() {
                    sala.caminoA(mesa);
                }
            }));
            int i = grafo.indiceDe(mesa);
            if (i > 0) {
                marcarCamino(i);
            }
            lienzo.repaint();
        }

        private String lista(Grafo.Arista[] aristas, int cantidad) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cantidad; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(grafo.nombre(aristas[i].u)).append("—").append(grafo.nombre(aristas[i].v))
                  .append(" (").append(aristas[i].peso).append(")");
            }
            return sb.length() == 0 ? "ninguna" : sb.toString();
        }

        private void conexion(boolean esPrim) {
            limpiar();
            Grafo.Conexion c = esPrim ? grafo.prim(grafo.indiceDe("R")) : grafo.kruskal();
            for (int i = 0; i < c.nAceptadas; i++) {
                marcar(c.aceptadas[i].u, c.aceptadas[i].v, ACEPTADA);
            }
            for (int i = 0; i < c.nRechazadas; i++) {
                marcar(c.rechazadas[i].u, c.rechazadas[i].v, RECHAZADA);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(esPrim ? "Prim desde R" : "Kruskal").append(", costo ").append(c.costo).append(":\n");
            sb.append("  acepta: ").append(lista(c.aceptadas, c.nAceptadas)).append("\n");
            sb.append(esPrim ? "  no usa: " : "  rechaza (cerrarían un ciclo): ")
              .append(lista(c.rechazadas, c.nRechazadas)).append("\n\n");
            sb.append("El cableado más barato une toda la sala; el camino de quien\n");
            sb.append("llega a jugar (Dijkstra) no tiene por qué usar las mismas aristas.\n");
            texto.setText(sb.toString());
            lienzo.repaint();
        }

        private void pintar(Graphics2D g, int w, int h) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0xF7F7F2));
            g.fillRect(0, 0, w, h);

            int n = grafo.tamano();
            int[] x = new int[n];
            int[] y = new int[n];
            for (int i = 0; i < n; i++) {
                x[i] = (int) (POS[i][0] * w);
                y[i] = (int) (POS[i][1] * h);
            }

            Grafo.Arista[] aristas = grafo.aristas();
            for (int k = 0; k < aristas.length; k++) {
                Grafo.Arista a = aristas[k];
                int tipo = estilo[a.u][a.v];
                switch (tipo) {
                    case CAMINO:
                        g.setColor(new Color(0x2E9E4F));
                        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        break;
                    case ACEPTADA:
                        g.setColor(new Color(0x1E66D0));
                        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        break;
                    case RECHAZADA:
                        g.setColor(new Color(0xD64545));
                        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                                new float[]{9f, 7f}, 0f));
                        break;
                    default:
                        g.setColor(new Color(0xA9A9A9));
                        g.setStroke(new BasicStroke(2.5f));
                        break;
                }
                g.drawLine(x[a.u], y[a.u], x[a.v], y[a.v]);
            }
            // pesos (minutos) sobre cada arista
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            java.awt.FontMetrics fm = g.getFontMetrics();
            for (int k = 0; k < aristas.length; k++) {
                Grafo.Arista a = aristas[k];
                int mx = (x[a.u] + x[a.v]) / 2;
                int my = (y[a.u] + y[a.v]) / 2;
                String s = a.peso + " min";
                int tw = fm.stringWidth(s) + 12;
                g.setColor(Color.WHITE);
                g.fillRoundRect(mx - tw / 2, my - 11, tw, 22, 10, 10);
                g.setColor(new Color(0x666666));
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(mx - tw / 2, my - 11, tw, 22, 10, 10);
                g.setColor(Color.BLACK);
                g.drawString(s, mx - fm.stringWidth(s) / 2, my + 5);
            }
            // nodos
            int r = 32;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            fm = g.getFontMetrics();
            for (int i = 0; i < n; i++) {
                Color relleno;
                if (i == 0) {
                    relleno = new Color(0x3F4B6B);
                } else if (sala.estaLlena(i)) {
                    relleno = new Color(0xC95454);
                } else {
                    relleno = new Color(0x2E9E4F);
                }
                g.setColor(relleno);
                g.fillOval(x[i] - r, y[i] - r, 2 * r, 2 * r);
                g.setColor(i == destino ? new Color(0xF2C94C) : Color.WHITE);
                g.setStroke(new BasicStroke(i == destino ? 6f : 3f));
                g.drawOval(x[i] - r, y[i] - r, 2 * r, 2 * r);
                g.setColor(Color.WHITE);
                String nom = grafo.nombre(i);
                g.drawString(nom, x[i] - fm.stringWidth(nom) / 2, y[i] + 6);
                if (i > 0) {
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    String est = sala.estaLlena(i) ? "llena" : "libre";
                    g.setColor(new Color(0x333333));
                    g.drawString(est, x[i] - g.getFontMetrics().stringWidth(est) / 2, y[i] + r + 16);
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                }
            }
            // leyenda
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            int ly = h - 14;
            g.setStroke(new BasicStroke(5f));
            g.setColor(new Color(0x2E9E4F));
            g.drawLine(12, ly, 42, ly);
            g.setColor(Color.DARK_GRAY);
            g.drawString("camino (Dijkstra)", 48, ly + 4);
            g.setColor(new Color(0x1E66D0));
            g.drawLine(170, ly, 200, ly);
            g.setColor(Color.DARK_GRAY);
            g.drawString("arista aceptada (Prim/Kruskal)", 206, ly + 4);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{9f, 7f}, 0f));
            g.setColor(new Color(0xD64545));
            g.drawLine(410, ly, 440, ly);
            g.setColor(Color.DARK_GRAY);
            g.drawString("arista rechazada", 446, ly + 4);
            g.dispose();
        }
    }

    // ==================================================================
    // Pestaña 3: salida completa de consola
    // ==================================================================

    static class CorridaPanel extends JPanel {
        private final JTextArea salida = areaDeTexto();
        private final PartidaPanel partida;

        CorridaPanel(PartidaPanel partida) {
            this.partida = partida;
            setLayout(new BorderLayout());

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            barra.add(accion("Corrida fija (verificación)", "fija"));
            barra.add(accion("Modo libre", "libre"));
            barra.add(accion("Solo la sala", "sala"));
            barra.add(accion("Árbol de decisión", "arbol"));
            JButton limpiar = boton("Limpiar");
            limpiar.addActionListener(e -> salida.setText(""));
            barra.add(limpiar);
            add(barra, BorderLayout.NORTH);
            add(new JScrollPane(salida), BorderLayout.CENTER);
            salida.setText("Pulsa «Corrida fija (verificación)» para ver la salida completa que se califica.\n");
        }

        private JButton accion(String texto, final String modo) {
            JButton b = boton(texto);
            b.addActionListener(e -> {
                if (partida.rondaEnCurso()) {
                    salida.setText("Termina la ronda de la pestaña «Partida» antes de correr esto.\n");
                    return;
                }
                salida.setText(capturar(new Runnable() {
                    public void run() {
                        Main.ejecutar(modo);
                    }
                }));
                salida.setCaretPosition(0);
            });
            return b;
        }
    }
}
