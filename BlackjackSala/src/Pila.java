/**
 * PILA sobre lista simple. Se usa para el DESCARTE (la ultima carta queda en la
 * cima) y para el HISTORIAL de acciones (deshacer retira solo la cima).
 * Entra y sale por el mismo extremo: la cima.
 */
public class Pila<T> {
    private NodoSimple<T> cima;
    private int tam;

    /** Pone el elemento en la cima. */
    public void apilar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<T>(dato);
        nuevo.sig = cima;
        cima = nuevo;
        tam++;
    }

    /** Retira la cima. Devuelve null si la pila esta vacia. */
    public T desapilar() {
        if (cima == null) {
            return null;
        }
        T dato = cima.dato;
        cima = cima.sig;
        tam--;
        return dato;
    }

    public T verCima() {
        return cima == null ? null : cima.dato;
    }

    public boolean estaVacia() {
        return cima == null;
    }

    public int tamano() {
        return tam;
    }

    /** Texto desde la cima hacia el fondo. */
    public String desdeLaCima() {
        StringBuilder sb = new StringBuilder();
        for (NodoSimple<T> n = cima; n != null; n = n.sig) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(n.dato);
        }
        return sb.toString();
    }
}
