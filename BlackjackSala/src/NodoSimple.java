/**
 * Nodo de lista simplemente enlazada. Lo usan la Cola (zapato) y la Pila
 * (descarte e historial).
 */
public class NodoSimple<T> {
    public T dato;
    public NodoSimple<T> sig;

    public NodoSimple(T dato) {
        this.dato = dato;
    }
}
