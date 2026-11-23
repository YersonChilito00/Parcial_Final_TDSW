/**
 * LISTA DOBLEMENTE ENLAZADA (anterior y siguiente). Se usa para la mano de cada
 * jugador y para los asientos de la mesa. Se puede leer de izquierda a derecha
 * (de cabeza a cola, siguiendo "sig") y al reves (de cola a cabeza, siguiendo
 * "ant").
 */
public class ListaDoble<T> {
    private NodoDoble<T> cabeza;
    private NodoDoble<T> cola;
    private int tam;

    /** Agrega al final de la lista (la carta nueva se cuelga al final). */
    public void agregarFinal(T dato) {
        NodoDoble<T> nuevo = new NodoDoble<T>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
            cola = nuevo;
        } else {
            nuevo.ant = cola;
            cola.sig = nuevo;
            cola = nuevo;
        }
        tam++;
    }

    public void limpiar() {
        cabeza = null;
        cola = null;
        tam = 0;
    }

    public NodoDoble<T> getCabeza() {
        return cabeza;
    }

    public NodoDoble<T> getCola() {
        return cola;
    }

    public T primero() {
        return cabeza == null ? null : cabeza.dato;
    }

    public int tamano() {
        return tam;
    }

    public boolean estaVacia() {
        return tam == 0;
    }

    /** Recorrido de izquierda a derecha (usa sig). */
    public String haciaLaDerecha(String separador) {
        StringBuilder sb = new StringBuilder();
        for (NodoDoble<T> n = cabeza; n != null; n = n.sig) {
            if (sb.length() > 0) {
                sb.append(separador);
            }
            sb.append(n.dato);
        }
        return sb.toString();
    }

    /** Recorrido al reves, de derecha a izquierda (usa ant). */
    public String haciaLaIzquierda(String separador) {
        StringBuilder sb = new StringBuilder();
        for (NodoDoble<T> n = cola; n != null; n = n.ant) {
            if (sb.length() > 0) {
                sb.append(separador);
            }
            sb.append(n.dato);
        }
        return sb.toString();
    }
}
