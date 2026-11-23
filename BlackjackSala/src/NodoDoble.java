/**
 * Nodo de lista doblemente enlazada: tiene anterior (ant) y siguiente (sig).
 * Lo usan la mano de cada jugador y los asientos de la mesa.
 */
public class NodoDoble<T> {
    public T dato;
    public NodoDoble<T> ant;
    public NodoDoble<T> sig;

    public NodoDoble(T dato) {
        this.dato = dato;
    }
}
