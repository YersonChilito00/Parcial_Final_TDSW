/**
 * La mesa de blackjack. Aqui se ven operar todas las estructuras:
 *
 *  - zapato      : Cola (la carta sale por el FRENTE)
 *  - descarte    : Pila (la carta entra por la CIMA)
 *  - historial   : Pila de acciones (deshacer retira solo la cima)
 *  - asientos    : ListaDoble de jugadores (derecha = orden del reparto)
 *  - mano        : ListaDoble de cartas (dentro de Mano)
 *  - jugadores   : TablaHash por identificador
 *  - ranking     : ArbolRanking (BST por fichas)
 *  - decisiones  : ArbolDecision (cada mano baja por si/no hasta una accion)
 *
 * Reglas: solo se puede pedir o plantarse (sin doblar, separar ni seguro). El
 * crupier pide con menos de 17 y se planta con 17 o mas (tambien con 17 con as).
 * Blackjack paga 3 a 2 salvo que el crupier tambien lo tenga (empate). Una
 * victoria normal paga 1 a 1.
 */
public class Mesa {
    private final ListaDoble<Jugador> asientos = new ListaDoble<Jugador>();
    private final TablaHash tabla = new TablaHash();
    private final ArbolDecision arbol = new ArbolDecision();
    private final Pila<Carta> descarte = new Pila<Carta>();
    private final Pila<String> historial = new Pila<String>();
    private final Cola<Carta> zapato;
    private final Cola<Boolean> guion; // decisiones "pide?" de la corrida fija (null en modo libre)
    private final Mano manoCrupier = new Mano();
    private ArbolRanking ranking;
    private int numRonda = 0;

    /** Quien decide "¿pide carta?" cuando juega una persona (interfaz gráfica). */
    public interface Decisor {
        boolean pide(Jugador j);
    }

    private Decisor decisor;           // null = consola / corrida fija con guion
    private boolean crupierRevelado;   // la carta oculta ya se mostró

    public Mesa(Cola<Carta> zapato, Cola<Boolean> guion) {
        this.zapato = zapato;
        this.guion = guion;
    }

    // ------------------------------------------------------------------
    // Asientos, tabla y apuestas
    // ------------------------------------------------------------------

    /** Sienta al jugador (a la derecha de los que ya estan) y lo guarda en la tabla. */
    public void sentar(Jugador j) {
        if (tabla.insertar(j)) {
            asientos.agregarFinal(j);
        }
    }

    public TablaHash getTabla() {
        return tabla;
    }

    public void imprimirAsientos() {
        System.out.println("Asientos (derecha, orden del reparto): " + asientos.haciaLaDerecha(" ↔ "));
        System.out.println("Al revés (izquierda):                  " + asientos.haciaLaIzquierda(" ↔ "));
    }

    /** Registra la apuesta de un jugador buscandolo en la tabla hash por su id. */
    public boolean apostar(String id, int monto) {
        Jugador j = tabla.buscar(id);
        if (j == null) {
            System.out.println("AVISO: el jugador " + id + " no existe; no se registra la apuesta.");
            return false;
        }
        if (monto <= 0 || monto > j.fichas) {
            System.out.println("AVISO: apuesta inválida de " + j.nombre + " (" + monto
                    + ", tiene " + j.fichas + " fichas).");
            return false;
        }
        j.apuesta = monto;
        return true;
    }

    // ------------------------------------------------------------------
    // Zapato
    // ------------------------------------------------------------------

    /** Saca la carta del frente del zapato. Si esta vacio avisa y devuelve null. */
    private Carta sacarCarta() {
        Carta c = zapato.desencolar();
        if (c == null) {
            System.out.println("    AVISO: el zapato está vacío; no se reparte ninguna carta.");
        }
        return c;
    }

    /**
     * Reparte una carta a una mano. Al cerrar la jugada de esa carta (ya esta
     * en la mano) pasa a la pila de descarte.
     */
    private Carta darCarta(Mano m) {
        Carta c = sacarCarta();
        if (c == null) {
            return null;
        }
        m.agregar(c);
        descarte.apilar(c);
        return c;
    }

    /** Prueba de borde: vacia el zapato y pide una carta a proposito. */
    public void probarZapatoVacio(Jugador j) {
        int retiradas = 0;
        while (zapato.desencolar() != null) {
            retiradas++;
        }
        System.out.println("Se retiran " + retiradas + " cartas del zapato para la prueba. Zapato vacío.");
        System.out.println(j.nombre + " pide carta:");
        Carta c = darCarta(j.mano);
        if (c == null) {
            System.out.println("    (no se inventó ninguna carta; la mano de " + j.nombre
                    + " sigue en " + j.mano.numCartas() + " cartas)");
        }
    }

    // ------------------------------------------------------------------
    // Una ronda
    // ------------------------------------------------------------------

    private boolean pideSegunGuion(Jugador j) {
        if (decisor != null) {
            return decisor.pide(j);
        }
        Boolean g = (guion == null) ? null : guion.desencolar();
        if (g != null) {
            return g.booleanValue();
        }
        return j.mano.getPuntaje() < 17; // modo libre: estrategia sencilla
    }

    private int contarApostadores() {
        int c = 0;
        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            if (n.dato.apuesta > 0) {
                c++;
            }
        }
        return c;
    }

    public void jugarRonda() {
        numRonda++;
        System.out.println();
        System.out.println("=== Ronda " + numRonda + " ===");

        int apostadores = contarApostadores();
        if (apostadores == 0) {
            System.out.println("Nadie apostó; no hay ronda.");
            return;
        }
        if (zapato.tamano() < 2 * (apostadores + 1)) {
            System.out.println("AVISO: el zapato no tiene cartas para repartir; se cancela la ronda.");
            for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
                n.dato.apuesta = 0;
            }
            return;
        }

        manoCrupier.limpiar();
        crupierRevelado = false;
        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            if (n.dato.apuesta > 0) { // quien no apuesta conserva su ultima mano
                n.dato.nuevaRonda();
            }
        }

        // anuncio de apuestas
        // Orden de la ronda: se empieza en el asiento que toca (rota un asiento por
        // ronda) y se avanza hacia la derecha; solo entran los que apostaron.
        Jugador[] orden = ordenDeRonda();

        StringBuilder ap = new StringBuilder();
        for (int i = 0; i < orden.length; i++) {
            if (i > 0) {
                ap.append(", ");
            }
            ap.append(orden[i].nombre).append(" ").append(orden[i].apuesta);
        }
        StringBuilder no = new StringBuilder();
        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            if (n.dato.apuesta <= 0) {
                if (no.length() > 0) {
                    no.append(", ");
                }
                no.append(n.dato.nombre);
            }
        }
        System.out.println("Apuestan: " + ap + (no.length() > 0 ? ". No apuesta: " + no + "." : "."));
        System.out.println("Orden de reparto y de turnos: " + textoOrden(orden) + " y el crupier.");

        // reparto: una carta a cada apostador, una al crupier, y otra vuelta
        for (int vuelta = 0; vuelta < 2; vuelta++) {
            for (int i = 0; i < orden.length; i++) {
                darCarta(orden[i].mano);
            }
            darCarta(manoCrupier);
        }
        for (int i = 0; i < orden.length; i++) {
            Jugador j = orden[i];
            System.out.println("  " + j.nombre + " recibe: " + j.mano.enLetras() + " = " + j.mano.getPuntaje());
        }
        Carta visible = manoCrupier.primera();
        System.out.println("  Crupier muestra: " + visible + " (la segunda carta queda oculta)");

        // el crupier mira la oculta si muestra as o una carta que vale 10
        boolean crupierBJ = false;
        if (visible.getValorBase() >= 10) {
            System.out.println("  El crupier muestra " + visible + ": mira su carta oculta.");
            if (manoCrupier.esBlackjack()) {
                crupierBJ = true;
                crupierRevelado = true;
                System.out.println("  El crupier tiene BLACKJACK: " + manoCrupier.enLetras());
                historial.apilar("El crupier tiene blackjack");
            } else {
                System.out.println("  El crupier no tiene blackjack.");
            }
        }

        if (crupierBJ) {
            resolverContraBlackjackDelCrupier(orden);
        } else {
            for (int i = 0; i < orden.length; i++) {
                turnoJugador(orden[i]);
            }
            boolean quedaAlguien = false;
            for (int i = 0; i < orden.length; i++) {
                if (!orden[i].paso) {
                    quedaAlguien = true;
                }
            }
            if (quedaAlguien) {
                turnoCrupier();
                liquidarPlantados(orden);
            } else {
                System.out.println("  Todos se pasaron: el crupier no juega.");
            }
        }

        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            n.dato.apuesta = 0;
        }
        imprimirFichas();
    }

    /**
     * Jugadores que apostaron, en el orden de la ronda. La ronda 1 empieza en
     * el primer asiento, la 2 en el segundo, etc. (da la vuelta al final).
     */
    private Jugador[] ordenDeRonda() {
        int sentados = asientos.tamano();
        int inicio = (numRonda - 1) % sentados;
        NodoDoble<Jugador> actual = asientos.getCabeza();
        for (int i = 0; i < inicio; i++) {
            actual = actual.sig;
        }
        Jugador[] res = new Jugador[contarApostadores()];
        int k = 0;
        for (int i = 0; i < sentados; i++) {
            if (actual.dato.apuesta > 0) {
                res[k++] = actual.dato;
            }
            actual = (actual.sig == null) ? asientos.getCabeza() : actual.sig;
        }
        return res;
    }

    private String textoOrden(Jugador[] orden) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orden.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(orden[i].nombre);
        }
        return sb.toString();
    }

    private void resolverContraBlackjackDelCrupier(Jugador[] orden) {
        for (int i = 0; i < orden.length; i++) {
            Jugador j = orden[i];
            if (j.mano.esBlackjack()) {
                System.out.println("  " + j.nombre + ": " + j.mano.enLetras() + ". Blackjack contra blackjack: empate, se devuelve la apuesta. Fichas " + j.fichas);
            } else {
                j.fichas -= j.apuesta;
                System.out.println("  " + j.nombre + ": " + j.mano.enLetras() + " = " + j.mano.getPuntaje()
                        + ". Pierde " + j.apuesta + " contra el blackjack del crupier. Fichas " + j.fichas);
            }
        }
    }

    private void turnoJugador(final Jugador j) {
        System.out.println("  Turno de " + j.nombre + ": " + j.mano.enLetras() + " = " + j.mano.getPuntaje());
        ArbolDecision.Eleccion eleccion = new ArbolDecision.Eleccion() {
            public boolean pideCarta() {
                return pideSegunGuion(j);
            }
        };
        boolean sigue = true;
        while (sigue) {
            StringBuilder ruta = new StringBuilder();
            ArbolDecision.Accion accion = arbol.decidir(j.mano, true, eleccion, ruta);
            System.out.println("    [árbol] " + ruta);
            switch (accion) {
                case PASARSE:
                    j.paso = true;
                    j.fichas -= j.apuesta;
                    historial.apilar(j.nombre + " se pasa con " + j.mano.getPuntaje());
                    System.out.println("    " + j.nombre + ": " + j.mano.enLetras() + ". " + j.mano.getPuntaje()
                            + ". Se pasa y pierde " + j.apuesta + ". Fichas " + j.fichas);
                    sigue = false;
                    break;
                case BLACKJACK:
                    j.blackjack = true;
                    int ganancia = j.apuesta * 3 / 2;
                    j.fichas += ganancia;
                    historial.apilar(j.nombre + " tiene blackjack");
                    System.out.println("    " + j.nombre + ": " + j.mano.enLetras()
                            + ". Blackjack. Cobra 3 a 2: +" + ganancia + ". Fichas " + j.fichas);
                    sigue = false;
                    break;
                case PEDIR:
                    Carta c = darCarta(j.mano);
                    if (c == null) {
                        historial.apilar(j.nombre + " se planta en " + j.mano.getPuntaje() + " (zapato vacío)");
                        System.out.println("    " + j.nombre + " no puede pedir: se planta en " + j.mano.getPuntaje());
                        sigue = false;
                    } else {
                        historial.apilar(j.nombre + " pide carta (" + c + ")");
                        System.out.println("    " + j.nombre + " pide: " + c + " → " + j.mano.getPuntaje());
                    }
                    break;
                default: // PLANTARSE
                    historial.apilar(j.nombre + " se planta en " + j.mano.getPuntaje());
                    System.out.println("    " + j.nombre + ": " + j.mano.enLetras() + ". " + j.mano.getPuntaje()
                            + ". Se planta.");
                    sigue = false;
                    break;
            }
        }
    }

    private void turnoCrupier() {
        crupierRevelado = true;
        System.out.println("  Turno del crupier. Revela: " + manoCrupier.enLetras() + " = " + manoCrupier.getPuntaje());
        boolean sigue = true;
        while (sigue) {
            StringBuilder ruta = new StringBuilder();
            ArbolDecision.Accion accion = arbol.decidir(manoCrupier, false, null, ruta);
            System.out.println("    [árbol] " + ruta);
            switch (accion) {
                case PASARSE:
                    historial.apilar("El crupier se pasa con " + manoCrupier.getPuntaje());
                    System.out.println("    El crupier se pasa con " + manoCrupier.getPuntaje());
                    sigue = false;
                    break;
                case CRUPIER_PIDE:
                    Carta c = darCarta(manoCrupier);
                    if (c == null) {
                        historial.apilar("El crupier se planta en " + manoCrupier.getPuntaje() + " (zapato vacío)");
                        System.out.println("    El crupier no puede pedir: se planta en " + manoCrupier.getPuntaje());
                        sigue = false;
                    } else {
                        historial.apilar("El crupier pide carta (" + c + ")");
                        System.out.println("    El crupier pide: " + c + " → " + manoCrupier.getPuntaje());
                    }
                    break;
                default: // CRUPIER_SE_PLANTA (el BLACKJACK ya se resolvio al mirar la oculta)
                    historial.apilar("El crupier se planta en " + manoCrupier.getPuntaje());
                    System.out.println("    El crupier: " + manoCrupier.enLetras() + ". " + manoCrupier.getPuntaje()
                            + ". Se planta.");
                    sigue = false;
                    break;
            }
        }
    }

    /** Compara con el crupier a quienes se plantaron (los blackjack y los pasados ya se resolvieron). */
    private void liquidarPlantados(Jugador[] orden) {
        int dealer = manoCrupier.getPuntaje();
        boolean dealerPaso = manoCrupier.sePaso();
        for (int i = 0; i < orden.length; i++) {
            Jugador j = orden[i];
            if (j.paso || j.blackjack) {
                continue;
            }
            int pts = j.mano.getPuntaje();
            String resultado;
            if (dealerPaso || pts > dealer) {
                j.fichas += j.apuesta;
                resultado = "Gana " + j.apuesta + (dealerPaso ? " (el crupier se pasó)" : "");
            } else if (pts == dealer) {
                resultado = "Empata y recupera la apuesta";
            } else {
                j.fichas -= j.apuesta;
                resultado = "Pierde " + j.apuesta;
            }
            System.out.println("  " + j.nombre + " con " + pts + " contra " + (dealerPaso ? "el " + dealer + " del crupier (pasado)" : dealer)
                    + ": " + resultado + ". Fichas " + j.fichas);
        }
    }

    public void imprimirFichas() {
        StringBuilder sb = new StringBuilder("  Fichas:");
        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            sb.append(" ").append(n.dato.nombre).append(" ").append(n.dato.fichas);
            if (n.sig != null) {
                sb.append(",");
            }
        }
        System.out.println(sb);
    }

    // ------------------------------------------------------------------
    // Consultas pedidas por el enunciado
    // ------------------------------------------------------------------

    /** Mano de un jugador en los dos sentidos (lista doble). */
    public void imprimirMano(String id) {
        Jugador j = tabla.buscar(id);
        if (j == null) {
            System.out.println("AVISO: el jugador " + id + " no existe.");
            return;
        }
        System.out.println("Mano de " + j.nombre + ": " + j.mano.deIzquierdaADerecha());
        System.out.println("Mano de " + j.nombre + " al revés: " + j.mano.alReves());
    }

    public void imprimirDescarte() {
        System.out.println("Descarte: " + descarte.tamano() + " cartas. Cima: " + descarte.verCima());
    }

    public void imprimirHistorial() {
        System.out.println("Historial: " + historial.tamano() + " acciones. Cima: " + historial.verCima());
    }

    /** Deshacer: retira solo la cima del historial. */
    public void deshacer() {
        String retirada = historial.desapilar();
        if (retirada == null) {
            System.out.println("AVISO: no hay acciones que deshacer.");
            return;
        }
        System.out.println("Al retirarla (" + retirada + "), la cima queda en: "
                + (historial.verCima() == null ? "(historial vacío)" : historial.verCima()));
    }

    /** Arma el ranking insertando las fichas finales en el orden de los ids dado. */
    public void armarRanking(String... ids) {
        ranking = new ArbolRanking();
        for (String id : ids) {
            Jugador j = tabla.buscar(id);
            if (j == null) {
                System.out.println("AVISO: el jugador " + id + " no existe; no entra al ranking.");
                continue;
            }
            ranking.insertar(j.fichas, j.nombre);
        }
    }

    /** Arma el ranking en el orden de los asientos (modo libre). */
    public void armarRankingPorAsientos() {
        ranking = new ArbolRanking();
        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            ranking.insertar(n.dato.fichas, n.dato.nombre);
        }
    }

    public void imprimirRanking(int buscado) {
        if (ranking == null) {
            System.out.println("AVISO: el ranking aún no se ha armado.");
            return;
        }
        ranking.imprimirAnalisis(buscado);
    }

    public int fichasDe(String id) {
        Jugador j = tabla.buscar(id);
        return j == null ? -1 : j.fichas;
    }

    // ------------------------------------------------------------------
    // Consultas para la interfaz gráfica
    // ------------------------------------------------------------------

    public void setDecisor(Decisor d) {
        this.decisor = d;
    }

    /** Los jugadores sentados, de izquierda a derecha. */
    public Jugador[] jugadores() {
        Jugador[] res = new Jugador[asientos.tamano()];
        int i = 0;
        for (NodoDoble<Jugador> n = asientos.getCabeza(); n != null; n = n.sig) {
            res[i++] = n.dato;
        }
        return res;
    }

    public Mano getManoCrupier() {
        return manoCrupier;
    }

    public boolean isCrupierRevelado() {
        return crupierRevelado;
    }

    public int cartasEnZapato() {
        return zapato.tamano();
    }

    public int cartasEnDescarte() {
        return descarte.tamano();
    }

    public ArbolDecision getArbolDecision() {
        return arbol;
    }
}
