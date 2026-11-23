/**
 * GRAFO NO DIRIGIDO Y PONDERADO guardado con LISTAS DE ADYACENCIA (cada vertice
 * tiene su propia lista simple de vecinos). Trae Dijkstra, Prim y Kruskal.
 * El peso de una arista es el tiempo de caminata, en minutos.
 */
public class Grafo {

    /** Nodo de la lista de adyacencia de un vertice. */
    private static class NodoAdy {
        int destino;
        int peso;
        NodoAdy sig;

        NodoAdy(int destino, int peso) {
            this.destino = destino;
            this.peso = peso;
        }
    }

    /** Una arista (u, v) con su peso. */
    public static class Arista {
        public final int u;
        public final int v;
        public final int peso;

        public Arista(int u, int v, int peso) {
            this.u = u;
            this.v = v;
            this.peso = peso;
        }
    }

    /** Resultado de Prim o Kruskal. */
    public static class Conexion {
        public Arista[] aceptadas;
        public int nAceptadas;
        public Arista[] rechazadas;
        public int nRechazadas;
        public int costo;
    }

    /** Resultado de Dijkstra. */
    public static class CaminoMinimo {
        public int origen;
        public int[] dist;
        public int[] previo;
    }

    public static final int INFINITO = Integer.MAX_VALUE / 2;

    private final String[] nombres;
    private final NodoAdy[] cabeza;
    private final NodoAdy[] cola;

    public Grafo(String[] nombres) {
        this.nombres = nombres;
        this.cabeza = new NodoAdy[nombres.length];
        this.cola = new NodoAdy[nombres.length];
    }

    public int tamano() {
        return nombres.length;
    }

    public String nombre(int i) {
        return nombres[i];
    }

    /** Indice del vertice, o -1 si no existe en el grafo. */
    public int indiceDe(String nombre) {
        for (int i = 0; i < nombres.length; i++) {
            if (nombres[i].equals(nombre)) {
                return i;
            }
        }
        return -1;
    }

    private void agregarAdy(int de, int a, int peso) {
        NodoAdy n = new NodoAdy(a, peso);
        if (cabeza[de] == null) {
            cabeza[de] = n;
        } else {
            cola[de].sig = n;
        }
        cola[de] = n;
    }

    public void agregarArista(String a, String b, int peso) {
        int i = indiceDe(a);
        int j = indiceDe(b);
        agregarAdy(i, j, peso);
        agregarAdy(j, i, peso);
    }

    /** Todas las aristas, cada una una sola vez (u < v), recorriendo las listas. */
    public Arista[] aristas() {
        int total = 0;
        for (int u = 0; u < nombres.length; u++) {
            for (NodoAdy n = cabeza[u]; n != null; n = n.sig) {
                if (u < n.destino) {
                    total++;
                }
            }
        }
        Arista[] res = new Arista[total];
        int k = 0;
        for (int u = 0; u < nombres.length; u++) {
            for (NodoAdy n = cabeza[u]; n != null; n = n.sig) {
                if (u < n.destino) {
                    res[k++] = new Arista(u, n.destino, n.peso);
                }
            }
        }
        return res;
    }

    // ------------------------------------------------------------------
    // Dijkstra
    // ------------------------------------------------------------------

    public CaminoMinimo dijkstra(int origen) {
        int n = nombres.length;
        CaminoMinimo cm = new CaminoMinimo();
        cm.origen = origen;
        cm.dist = new int[n];
        cm.previo = new int[n];
        boolean[] fijo = new boolean[n];
        for (int i = 0; i < n; i++) {
            cm.dist[i] = INFINITO;
            cm.previo[i] = -1;
        }
        cm.dist[origen] = 0;
        for (int paso = 0; paso < n; paso++) {
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!fijo[i] && cm.dist[i] < INFINITO && (u == -1 || cm.dist[i] < cm.dist[u])) {
                    u = i;
                }
            }
            if (u == -1) {
                break;
            }
            fijo[u] = true;
            for (NodoAdy a = cabeza[u]; a != null; a = a.sig) {
                int nueva = cm.dist[u] + a.peso;
                // "<" estricto: en un empate se conserva el camino que ya estaba
                if (nueva < cm.dist[a.destino]) {
                    cm.dist[a.destino] = nueva;
                    cm.previo[a.destino] = u;
                }
            }
        }
        return cm;
    }

    /** Camino "R → T1 → T3" desde el origen hasta destino, o null si no hay. */
    public String camino(CaminoMinimo cm, int destino) {
        if (cm.dist[destino] >= INFINITO) {
            return null;
        }
        int largo = 0;
        for (int v = destino; v != -1; v = cm.previo[v]) {
            largo++;
        }
        int[] pasos = new int[largo];
        int k = largo - 1;
        for (int v = destino; v != -1; v = cm.previo[v]) {
            pasos[k--] = v;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < largo; i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(nombres[pasos[i]]);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Prim
    // ------------------------------------------------------------------

    /**
     * Prim desde un vertice: en cada paso agrega la arista mas barata que une
     * el arbol con un vertice que aun no esta en el.
     */
    public Conexion prim(int origen) {
        Arista[] todas = aristas();
        int n = nombres.length;
        boolean[] enArbol = new boolean[n];
        boolean[] usada = new boolean[todas.length];
        Conexion res = new Conexion();
        res.aceptadas = new Arista[n];
        res.rechazadas = new Arista[todas.length];
        enArbol[origen] = true;
        for (int paso = 1; paso < n; paso++) {
            int mejor = -1;
            for (int e = 0; e < todas.length; e++) {
                if (enArbol[todas[e].u] != enArbol[todas[e].v]
                        && (mejor == -1 || todas[e].peso < todas[mejor].peso)) {
                    mejor = e;
                }
            }
            if (mejor == -1) {
                break; // grafo no conexo
            }
            Arista a = todas[mejor];
            usada[mejor] = true;
            // se guarda orientada: desde el vertice que ya estaba en el arbol
            Arista orientada = enArbol[a.u] ? new Arista(a.u, a.v, a.peso) : new Arista(a.v, a.u, a.peso);
            res.aceptadas[res.nAceptadas++] = orientada;
            res.costo += a.peso;
            enArbol[orientada.v] = true;
        }
        for (int e = 0; e < todas.length; e++) {
            if (!usada[e]) {
                res.rechazadas[res.nRechazadas++] = todas[e];
            }
        }
        return res;
    }

    // ------------------------------------------------------------------
    // Kruskal
    // ------------------------------------------------------------------

    /**
     * Kruskal: ordena las aristas por peso y acepta la siguiente si no cierra
     * un ciclo (se comprueba con conjuntos disjuntos). Las que cierran ciclo
     * se rechazan.
     */
    public Conexion kruskal() {
        Arista[] orden = aristas();
        // ordenamiento por insercion (estable)
        for (int i = 1; i < orden.length; i++) {
            Arista x = orden[i];
            int j = i - 1;
            while (j >= 0 && orden[j].peso > x.peso) {
                orden[j + 1] = orden[j];
                j--;
            }
            orden[j + 1] = x;
        }
        int n = nombres.length;
        int[] padre = new int[n];
        for (int i = 0; i < n; i++) {
            padre[i] = i;
        }
        Conexion res = new Conexion();
        res.aceptadas = new Arista[n];
        res.rechazadas = new Arista[orden.length];
        for (int e = 0; e < orden.length; e++) {
            int ru = raiz(padre, orden[e].u);
            int rv = raiz(padre, orden[e].v);
            if (ru != rv) {
                padre[ru] = rv;
                res.aceptadas[res.nAceptadas++] = orden[e];
                res.costo += orden[e].peso;
            } else {
                res.rechazadas[res.nRechazadas++] = orden[e];
            }
        }
        return res;
    }

    private int raiz(int[] padre, int x) {
        while (padre[x] != x) {
            padre[x] = padre[padre[x]];
            x = padre[x];
        }
        return x;
    }
}
