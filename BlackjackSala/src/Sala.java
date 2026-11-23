/**
 * La sala: una recepcion (R) y cuatro mesas (T1..T4) unidas por pasillos. El
 * peso de cada arista es el tiempo de caminata en minutos.
 *
 *  - Dijkstra responde que mesa LIBRE queda mas cerca de la recepcion.
 *  - Prim y Kruskal responden otra cosa: como quedarian unidas todas las
 *    mesas si se tendiera el cableado mas barato.
 */
public class Sala {
    private final Grafo grafo;
    private final boolean[] llena;

    public Sala() {
        grafo = new Grafo(new String[]{"R", "T1", "T2", "T3", "T4"});
        grafo.agregarArista("R", "T1", 4);
        grafo.agregarArista("R", "T2", 6);
        grafo.agregarArista("T1", "T2", 2);
        grafo.agregarArista("T1", "T3", 5);
        grafo.agregarArista("T2", "T4", 3);
        grafo.agregarArista("T3", "T4", 1);
        grafo.agregarArista("T2", "T3", 8);
        llena = new boolean[grafo.tamano()];
    }

    public Grafo getGrafo() {
        return grafo;
    }

    public boolean estaLlena(int indice) {
        return llena[indice];
    }

    /** Marca una mesa como llena o libre. */
    public void marcarLlena(String mesa, boolean estado) {
        int i = grafo.indiceDe(mesa);
        if (i <= 0) { // 0 es la recepcion
            System.out.println("AVISO: la mesa " + mesa + " no existe en la sala.");
            return;
        }
        llena[i] = estado;
    }

    /** Dijkstra desde R: imprime las distancias y devuelve la mesa libre mas cercana. */
    public String buscarMesaLibre() {
        int recepcion = grafo.indiceDe("R");
        Grafo.CaminoMinimo cm = grafo.dijkstra(recepcion);
        int mejor = -1;
        for (int i = 0; i < grafo.tamano(); i++) {
            if (i == recepcion) {
                continue;
            }
            String estado = llena[i] ? "llena" : "libre";
            System.out.println("  " + grafo.nombre(i) + " (" + estado + ") queda a " + cm.dist[i]
                    + " min: " + grafo.camino(cm, i));
            if (!llena[i] && (mejor == -1 || cm.dist[i] < cm.dist[mejor])) {
                mejor = i;
            }
        }
        if (mejor == -1) {
            System.out.println("AVISO: no hay ninguna mesa con puesto libre.");
            return null;
        }
        System.out.println("Dijkstra: la mesa libre más cercana es " + grafo.nombre(mejor) + ", "
                + grafo.camino(cm, mejor) + ", " + cm.dist[mejor] + " min");
        return grafo.nombre(mejor);
    }

    /** Camino desde la recepcion hasta una mesa dada. Avisa si la mesa no existe. */
    public void caminoA(String mesa) {
        int destino = grafo.indiceDe(mesa);
        if (destino <= 0) {
            System.out.println("AVISO: la mesa " + mesa + " no está en la sala; no hay camino.");
            return;
        }
        Grafo.CaminoMinimo cm = grafo.dijkstra(grafo.indiceDe("R"));
        System.out.println("Camino a " + mesa + ": " + grafo.camino(cm, destino) + ", "
                + cm.dist[destino] + " min");
    }

    /** Prim y Kruskal: el cableado mas barato que une toda la sala. */
    public void conectarSala() {
        Grafo.Conexion prim = grafo.prim(grafo.indiceDe("R"));
        System.out.println("Prim desde R, costo " + prim.costo + ":");
        System.out.println("  acepta: " + texto(prim.aceptadas, prim.nAceptadas));
        System.out.println("  no usa: " + texto(prim.rechazadas, prim.nRechazadas));

        Grafo.Conexion kr = grafo.kruskal();
        System.out.println("Kruskal, costo " + kr.costo + ":");
        System.out.println("  acepta: " + texto(kr.aceptadas, kr.nAceptadas));
        System.out.println("  rechaza (cerrarían un ciclo): " + texto(kr.rechazadas, kr.nRechazadas));
    }

    private String texto(Grafo.Arista[] aristas, int cantidad) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cantidad; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(grafo.nombre(aristas[i].u)).append("—").append(grafo.nombre(aristas[i].v))
              .append(" (").append(aristas[i].peso).append(")");
        }
        return sb.length() == 0 ? "ninguna" : sb.toString();
    }
}
