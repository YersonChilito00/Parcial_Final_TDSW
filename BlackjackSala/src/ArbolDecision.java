/**
 * ARBOL BINARIO DE DECISION. No guarda cartas: cada nodo interno hace una
 * pregunta y tiene exactamente dos hijos (si / no); la hoja dice que pasa con
 * la mano. Se usa igual para el jugador y para el crupier.
 *
 *   ¿La mano pasa de 21?
 *   ├─ sí → se pasa y pierde la apuesta
 *   └─ no → ¿son dos cartas y suman 21?
 *            ├─ sí → blackjack (cobra 3 a 2, o empata si el crupier tambien lo tiene)
 *            └─ no → ¿le toca decidir al jugador?
 *                     ├─ sí → ¿pide carta?
 *                     │        ├─ sí → pedir (sale una carta del zapato y se reevalua)
 *                     │        └─ no → plantarse
 *                     └─ no → ¿el crupier tiene menos de 17?
 *                              ├─ sí → el crupier pide carta
 *                              └─ no → el crupier se planta y se comparan puntajes
 */
public class ArbolDecision {

    /** Las acciones posibles (las hojas del arbol). */
    public enum Accion {
        PASARSE("pasarse (pierde la apuesta)"),
        BLACKJACK("blackjack (cobra 3 a 2, o empata si el crupier también lo tiene)"),
        PEDIR("pedir carta"),
        PLANTARSE("plantarse"),
        CRUPIER_PIDE("el crupier pide carta"),
        CRUPIER_SE_PLANTA("el crupier se planta y se comparan los puntajes");

        private final String texto;

        Accion(String texto) {
            this.texto = texto;
        }

        public String getTexto() {
            return texto;
        }
    }

    /** Se consulta SOLO cuando el arbol llega a la pregunta "¿pide carta?". */
    public interface Eleccion {
        boolean pideCarta();
    }

    // criterios de las preguntas
    private static final int PASA_21 = 0;
    private static final int DOS_CARTAS_21 = 1;
    private static final int TURNO_JUGADOR = 2;
    private static final int PIDE = 3;
    private static final int CRUPIER_MENOR_17 = 4;

    private static class NodoDecision {
        int criterio;
        String texto;
        Accion accion; // solo en hojas
        NodoDecision si;
        NodoDecision no;

        boolean esHoja() {
            return accion != null;
        }
    }

    private final NodoDecision raiz;

    public ArbolDecision() {
        raiz = pregunta(PASA_21, "¿La mano pasa de 21?",
                hoja(Accion.PASARSE),
                pregunta(DOS_CARTAS_21, "¿Son dos cartas y suman 21?",
                        hoja(Accion.BLACKJACK),
                        pregunta(TURNO_JUGADOR, "¿Le toca decidir al jugador?",
                                pregunta(PIDE, "¿Pide carta?",
                                        hoja(Accion.PEDIR),
                                        hoja(Accion.PLANTARSE)),
                                pregunta(CRUPIER_MENOR_17, "¿El crupier tiene menos de 17?",
                                        hoja(Accion.CRUPIER_PIDE),
                                        hoja(Accion.CRUPIER_SE_PLANTA)))));
    }

    private static NodoDecision pregunta(int criterio, String texto, NodoDecision si, NodoDecision no) {
        NodoDecision n = new NodoDecision();
        n.criterio = criterio;
        n.texto = texto;
        n.si = si;
        n.no = no;
        return n;
    }

    private static NodoDecision hoja(Accion a) {
        NodoDecision n = new NodoDecision();
        n.accion = a;
        return n;
    }

    /**
     * Baja por el arbol con la mano dada hasta una hoja.
     *
     * @param ruta si no es null, recibe las preguntas con su respuesta
     */
    public Accion decidir(Mano mano, boolean esJugador, Eleccion eleccion, StringBuilder ruta) {
        NodoDecision n = raiz;
        while (!n.esHoja()) {
            boolean respuesta = responder(n.criterio, mano, esJugador, eleccion);
            if (ruta != null) {
                if (ruta.length() > 0) {
                    ruta.append(" → ");
                }
                ruta.append(n.texto).append(respuesta ? " sí" : " no");
            }
            n = respuesta ? n.si : n.no;
        }
        if (ruta != null) {
            ruta.append(" ⇒ ").append(n.accion.getTexto());
        }
        return n.accion;
    }

    private boolean responder(int criterio, Mano mano, boolean esJugador, Eleccion eleccion) {
        switch (criterio) {
            case PASA_21:
                return mano.getPuntaje() > 21;
            case DOS_CARTAS_21:
                return mano.esBlackjack();
            case TURNO_JUGADOR:
                return esJugador;
            case PIDE:
                return eleccion != null && eleccion.pideCarta();
            case CRUPIER_MENOR_17:
                return mano.getPuntaje() < 17;
            default:
                return false;
        }
    }

    // ---- impresion del arbol ----

    public void imprimir() {
        imprimir(raiz, "  ", "");
    }

    private void imprimir(NodoDecision n, String prefijo, String etiqueta) {
        String texto = n.esHoja() ? n.accion.getTexto() : n.texto;
        System.out.println(prefijo + etiqueta + texto);
        if (n.esHoja()) {
            return;
        }
        imprimir(n.si, prefijo + "   ", "sí → ");
        imprimir(n.no, prefijo + "   ", "no → ");
    }
}
