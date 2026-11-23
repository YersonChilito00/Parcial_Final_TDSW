/**
 * ARBOL BINARIO DE BUSQUEDA: ranking por fichas al cerrar la corrida.
 * Menores a la izquierda; iguales o mayores a la derecha. No se usa TreeMap.
 *
 * Definiciones del enunciado:
 *  - nivel: numero de aristas desde la raiz.
 *  - grado: numero de hijos de un nodo.
 *  - peso: numero de nodos SIN contar la raiz.
 *  - altura: aristas del camino mas largo hasta una hoja (un arbol de una sola
 *    raiz tiene altura 0).
 *  - completo: cada nodo tiene cero hijos o dos.
 */
public class ArbolRanking {
    private static class Nodo {
        int fichas;
        String nombre;
        Nodo izq;
        Nodo der;

        Nodo(int fichas, String nombre) {
            this.fichas = fichas;
            this.nombre = nombre;
        }
    }

    private Nodo raiz;
    private int nodos;

    public void insertar(int fichas, String nombre) {
        Nodo nuevo = new Nodo(fichas, nombre);
        nodos++;
        if (raiz == null) {
            raiz = nuevo;
            return;
        }
        Nodo actual = raiz;
        while (true) {
            if (fichas < actual.fichas) {
                if (actual.izq == null) {
                    actual.izq = nuevo;
                    return;
                }
                actual = actual.izq;
            } else {
                if (actual.der == null) {
                    actual.der = nuevo;
                    return;
                }
                actual = actual.der;
            }
        }
    }

    private Nodo buscar(int fichas) {
        Nodo actual = raiz;
        while (actual != null && actual.fichas != fichas) {
            actual = fichas < actual.fichas ? actual.izq : actual.der;
        }
        return actual;
    }

    // ---- recorridos y medidas ----

    public String inorden() {
        StringBuilder sb = new StringBuilder();
        inorden(raiz, sb);
        return sb.toString();
    }

    private void inorden(Nodo n, StringBuilder sb) {
        if (n == null) {
            return;
        }
        inorden(n.izq, sb);
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(n.fichas);
        inorden(n.der, sb);
    }

    /** Camino desde la raiz hasta el nodo, o null si no esta. */
    public String camino(int fichas) {
        StringBuilder sb = new StringBuilder();
        Nodo actual = raiz;
        while (actual != null) {
            if (sb.length() > 0) {
                sb.append(" → ");
            }
            sb.append(actual.fichas);
            if (actual.fichas == fichas) {
                return sb.toString();
            }
            actual = fichas < actual.fichas ? actual.izq : actual.der;
        }
        return null;
    }

    /** Nivel del nodo (aristas desde la raiz); -1 si no existe. */
    public int nivel(int fichas) {
        int nivel = 0;
        Nodo actual = raiz;
        while (actual != null) {
            if (actual.fichas == fichas) {
                return nivel;
            }
            actual = fichas < actual.fichas ? actual.izq : actual.der;
            nivel++;
        }
        return -1;
    }

    /** Grado (numero de hijos) del nodo; -1 si no existe. */
    public int grado(int fichas) {
        Nodo n = buscar(fichas);
        if (n == null) {
            return -1;
        }
        return (n.izq != null ? 1 : 0) + (n.der != null ? 1 : 0);
    }

    public int totalNodos() {
        return nodos;
    }

    /** Peso: nodos sin contar la raiz. */
    public int peso() {
        return nodos > 0 ? nodos - 1 : 0;
    }

    /** Altura: un arbol de una sola raiz tiene altura 0 (vacio: -1). */
    public int altura() {
        return altura(raiz);
    }

    private int altura(Nodo n) {
        if (n == null) {
            return -1;
        }
        return 1 + Math.max(altura(n.izq), altura(n.der));
    }

    public int hojas() {
        return hojas(raiz);
    }

    private int hojas(Nodo n) {
        if (n == null) {
            return 0;
        }
        if (n.izq == null && n.der == null) {
            return 1;
        }
        return hojas(n.izq) + hojas(n.der);
    }

    /** Completo: todos los nodos tienen cero hijos o dos. */
    public boolean esCompleto() {
        return esCompleto(raiz);
    }

    private boolean esCompleto(Nodo n) {
        if (n == null) {
            return true;
        }
        if ((n.izq == null) != (n.der == null)) {
            return false;
        }
        return esCompleto(n.izq) && esCompleto(n.der);
    }

    // ---- impresion ----

    public void dibujar() {
        if (raiz == null) {
            System.out.println("  (árbol vacío)");
            return;
        }
        System.out.println("  " + raiz.fichas + " " + raiz.nombre);
        dibujarHijos(raiz, "  ");
    }

    private void dibujarHijos(Nodo n, String prefijo) {
        Nodo[] hijos = {n.izq, n.der};
        String[] lado = {"izq", "der"};
        int ultimo = -1;
        for (int i = 0; i < 2; i++) {
            if (hijos[i] != null) {
                ultimo = i;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (hijos[i] == null) {
                continue;
            }
            boolean esUltimo = (i == ultimo);
            System.out.println(prefijo + (esUltimo ? "└─ " : "├─ ") + lado[i] + ": "
                    + hijos[i].fichas + " " + hijos[i].nombre);
            dibujarHijos(hijos[i], prefijo + (esUltimo ? "   " : "│  "));
        }
    }

    /** Imprime todas las medidas pedidas y la busqueda de un valor. */
    public void imprimirAnalisis(int buscado) {
        dibujar();
        System.out.println("  Inorden: " + inorden());
        System.out.println("  Nodos: " + totalNodos() + "  Peso: " + peso() + "  Altura: " + altura()
                + "  Hojas: " + hojas() + "  Completo: " + (esCompleto() ? "sí" : "no"));
        String cam = camino(buscado);
        if (cam == null) {
            System.out.println("  Buscar " + buscado + ": no está en el árbol.");
        } else {
            System.out.println("  Buscar " + buscado + ": camino " + cam + ", nivel " + nivel(buscado)
                    + ", grado " + grado(buscado));
        }
    }
}
