import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Scanner;

/**
 * Parcial de Estructuras de Datos - Sala de blackjack.
 *
 * Uso:
 *   java Main            -> menu
 *   java Main fija       -> corrida de verificación (cartas fijas)
 *   java Main libre      -> modo libre (mazo barajado)
 *   java Main sala       -> solo la sala (Dijkstra, Prim y Kruskal)
 *   java Main gui        -> interfaz gráfica (también: java Interfaz)
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("gui")) {
            Interfaz.main(args);
            return;
        }

        // Para que se vean bien las flechas y las tildes en la terminal.
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
        } catch (Exception e) {
            // si falla se usa la salida normal
        }

        if (args.length > 0) {
            ejecutar(args[0]);
            return;
        }

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("===== SALA DE BLACKJACK =====");
            System.out.println("1. Corrida de verificación (cartas fijas)");
            System.out.println("2. Modo libre (mazo barajado)");
            System.out.println("3. Solo la sala: Dijkstra, Prim y Kruskal");
            System.out.println("4. Ver el árbol de decisión");
            System.out.println("5. Interfaz gráfica");
            System.out.println("0. Salir");
            System.out.print("Opción: ");
            if (!in.hasNextLine()) {
                break;
            }
            String op = in.nextLine().trim();
            if (op.equals("0")) {
                break;
            }
            if (op.equals("1")) {
                ejecutar("fija");
            } else if (op.equals("2")) {
                ejecutar("libre");
            } else if (op.equals("3")) {
                ejecutar("sala");
            } else if (op.equals("4")) {
                ejecutar("arbol");
            } else if (op.equals("5")) {
                Interfaz.main(new String[0]);
                break;
            } else {
                System.out.println("Opción no válida.");
            }
        }
    }

    static void ejecutar(String modo) {
        if (modo.equals("fija")) {
            corridaFija();
        } else if (modo.equals("libre")) {
            modoLibre();
        } else if (modo.equals("sala")) {
            soloSala();
        } else if (modo.equals("arbol")) {
            System.out.println("Árbol de decisión (cada nodo interno tiene dos hijos):");
            new ArbolDecision().imprimir();
        } else {
            System.out.println("Modo desconocido: " + modo + " (use fija, libre o sala).");
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("##### " + texto + " #####");
    }

    // ------------------------------------------------------------------
    // Sala
    // ------------------------------------------------------------------

    private static void soloSala() {
        titulo("LA SALA");
        Sala sala = new Sala();
        System.out.println("Sala, T1 libre:");
        sala.buscarMesaLibre();
        sala.conectarSala();
        sala.marcarLlena("T1", true);
        System.out.println("T1 llena:");
        sala.buscarMesaLibre();
        sala.caminoA("T9");
    }

    // ------------------------------------------------------------------
    // Corrida fija
    // ------------------------------------------------------------------

    private static void corridaFija() {
        titulo("1. LA SALA: Dijkstra hasta la mesa, Prim y Kruskal para conectar");
        Sala sala = new Sala();
        System.out.println("Sala, T1 libre:");
        sala.buscarMesaLibre();
        System.out.println();
        sala.conectarSala();
        System.out.println();
        sala.marcarLlena("T1", true);
        System.out.println("T1 llena:");
        sala.buscarMesaLibre();
        sala.marcarLlena("T1", false);
        System.out.println();
        System.out.println("Para la partida, T1 vuelve a quedar libre y ahí se sientan.");

        titulo("2. LA MESA: asientos (lista doble) y tabla hash");
        // decisiones "¿pide carta?" de la corrida: Luis pide; Marta se planta; Ana pide y luego se planta
        Cola<Boolean> guion = new Cola<Boolean>();
        guion.encolar(true);   // ronda 1, Luis (16): pide
        guion.encolar(false);  // ronda 2, Marta (17): se planta
        guion.encolar(true);   // ronda 2, Ana (13): pide
        guion.encolar(false);  // ronda 2, Ana (16): se planta
        Mesa mesa = new Mesa(new Baraja().zapatoDeCorrida(), guion);
        mesa.sentar(new Jugador("J01", "Ana", 500));
        mesa.sentar(new Jugador("J02", "Luis", 400));
        mesa.sentar(new Jugador("J06", "Marta", 300));
        mesa.imprimirAsientos();
        System.out.println("Tabla hash (cubeta = suma de códigos del id, módulo 5):");
        mesa.getTabla().imprimir();
        mesa.getTabla().buscarConTraza("J01");
        mesa.getTabla().buscarConTraza("J06");
        mesa.getTabla().buscarConTraza("J02");

        titulo("3. ÁRBOL DE DECISIÓN");
        mesa.getArbolDecision().imprimir();

        titulo("4. LAS DOS RONDAS");
        mesa.apostar("J01", 100);
        mesa.apostar("J02", 50);
        mesa.jugarRonda();
        System.out.println();
        mesa.imprimirMano("J02");

        mesa.apostar("J06", 80);
        mesa.apostar("J01", 100);
        mesa.jugarRonda();

        titulo("5. DESCARTE E HISTORIAL");
        mesa.imprimirDescarte();
        mesa.imprimirHistorial();
        mesa.deshacer();

        titulo("6. RANKING (árbol binario de búsqueda)");
        System.out.println("Se insertan las fichas finales en este orden: Marta, Ana, Luis.");
        mesa.armarRanking("J06", "J01", "J02");
        mesa.imprimirRanking(350);

        titulo("7. CASOS DE BORDE");
        mesa.getTabla().buscarConTraza("J99");
        sala.caminoA("T9");
        mesa.probarZapatoVacio(mesa.getTabla().buscar("J02"));
    }

    // ------------------------------------------------------------------
    // Modo libre
    // ------------------------------------------------------------------

    private static void modoLibre() {
        titulo("MODO LIBRE (mazo barajado)");
        Sala sala = new Sala();
        System.out.println("Búsqueda de mesa para los jugadores:");
        String mesaElegida = sala.buscarMesaLibre();
        System.out.println("Se sientan en " + mesaElegida + ".");

        Mesa mesa = new Mesa(new Baraja().zapatoBarajado(new Random()), null);
        mesa.sentar(new Jugador("J01", "Ana", 500));
        mesa.sentar(new Jugador("J02", "Luis", 400));
        mesa.sentar(new Jugador("J06", "Marta", 300));
        mesa.imprimirAsientos();

        for (int r = 0; r < 3; r++) {
            mesa.apostar("J01", 50);
            mesa.apostar("J02", 50);
            mesa.apostar("J06", 50);
            mesa.jugarRonda();
        }
        mesa.imprimirDescarte();
        mesa.imprimirHistorial();
        mesa.armarRankingPorAsientos();
        System.out.println("Ranking (se inserta en el orden de los asientos):");
        mesa.imprimirRanking(mesa.fichasDe("J01"));
    }
}
