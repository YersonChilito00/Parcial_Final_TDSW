/**
 * La mano de un jugador (o del crupier): una LISTA DOBLE de cartas. La carta
 * nueva se cuelga al final y la mano se puede leer en los dos sentidos.
 */
public class Mano {
    private final ListaDoble<Carta> cartas = new ListaDoble<Carta>();
    private int puntaje;

    public void agregar(Carta c) {
        cartas.agregarFinal(c);
        recalcular();
    }

    public void limpiar() {
        cartas.limpiar();
        puntaje = 0;
    }

    /**
     * Se repasa la mano completa cada vez que entra una carta: los ases
     * valen 11 y, mientras la mano pase de 21, un as pasa a valer 1.
     */
    private void recalcular() {
        int total = 0;
        int ases = 0;
        for (NodoDoble<Carta> n = cartas.getCabeza(); n != null; n = n.sig) {
            total += n.dato.getValorBase();
            if (n.dato.esAs()) {
                ases++;
            }
        }
        while (total > 21 && ases > 0) {
            total -= 10;
            ases--;
        }
        puntaje = total;
    }

    /** Las cartas de izquierda a derecha en un arreglo (para dibujarlas en la interfaz). */
    public Carta[] comoArreglo() {
        Carta[] res = new Carta[cartas.tamano()];
        int i = 0;
        for (NodoDoble<Carta> n = cartas.getCabeza(); n != null; n = n.sig) {
            res[i++] = n.dato;
        }
        return res;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public int numCartas() {
        return cartas.tamano();
    }

    public Carta primera() {
        return cartas.primero();
    }

    public boolean sePaso() {
        return puntaje > 21;
    }

    /** Blackjack: 21 con las dos primeras cartas. */
    public boolean esBlackjack() {
        return cartas.tamano() == 2 && puntaje == 21;
    }

    /** Izquierda a derecha, separado por comas. */
    public String deIzquierdaADerecha() {
        return cartas.haciaLaDerecha(", ");
    }

    /** Al reves (usa los enlaces "anterior"). */
    public String alReves() {
        return cartas.haciaLaIzquierda(", ");
    }

    /** "A, B y C" */
    public String enLetras() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int total = cartas.tamano();
        for (NodoDoble<Carta> n = cartas.getCabeza(); n != null; n = n.sig) {
            if (i > 0) {
                sb.append(i == total - 1 ? " y " : ", ");
            }
            sb.append(n.dato);
            i++;
        }
        return sb.toString();
    }
}
