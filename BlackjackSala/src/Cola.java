/**
 * COLA sobre lista simple. Es el ZAPATO: las cartas entran por el final
 * (encolar) y SALEN POR EL FRENTE (desencolar); la siguiente carta pasa a ser
 * el nuevo frente.
 */
public class Cola<T> {
    private NodoSimple<T> frente;
    private NodoSimple<T> fin;
    private int tam;

    /** Agrega por el final. */
    public void encolar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<T>(dato);
        if (fin == null) {
            frente = nuevo;
            fin = nuevo;
        } else {
            fin.sig = nuevo;
            fin = nuevo;
        }
        tam++;
    }

    /** Saca el elemento del frente. Devuelve null si la cola esta vacia. */
    public T desencolar() {
        if (frente == null) {
            return null;
        }
        T dato = frente.dato;
        frente = frente.sig;
        if (frente == null) {
            fin = null;
        }
        tam--;
        return dato;
    }

    public T verFrente() {
        return frente == null ? null : frente.dato;
    }

    public boolean estaVacia() {
        return frente == null;
    }

    public int tamano() {
        return tam;
    }
}
