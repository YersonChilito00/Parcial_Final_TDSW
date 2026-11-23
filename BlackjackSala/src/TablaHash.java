/**
 * TABLA HASH de jugadores por identificador. Cinco cubetas; cada cubeta es una
 * lista simple (encadenamiento). La cubeta se calcula como la suma de los
 * codigos de los caracteres del identificador, modulo 5.
 * No se usa HashMap.
 */
public class TablaHash {
    public static final int CUBETAS = 5;

    private static class NodoCubeta {
        Jugador jugador;
        NodoCubeta sig;

        NodoCubeta(Jugador j) {
            this.jugador = j;
        }
    }

    private final NodoCubeta[] cubetas = new NodoCubeta[CUBETAS];

    public static int sumaCodigos(String id) {
        int suma = 0;
        for (int i = 0; i < id.length(); i++) {
            suma += id.charAt(i);
        }
        return suma;
    }

    public static int cubetaDe(String id) {
        return sumaCodigos(id) % CUBETAS;
    }

    /** Inserta al final de la lista de su cubeta. */
    public boolean insertar(Jugador j) {
        if (buscar(j.id) != null) {
            System.out.println("AVISO: ya existe un jugador con el identificador " + j.id + ".");
            return false;
        }
        int c = cubetaDe(j.id);
        NodoCubeta nuevo = new NodoCubeta(j);
        if (cubetas[c] == null) {
            cubetas[c] = nuevo;
        } else {
            NodoCubeta a = cubetas[c];
            while (a.sig != null) {
                a = a.sig;
            }
            a.sig = nuevo;
        }
        return true;
    }

    /** Busca recorriendo la lista de la cubeta. Devuelve null si no existe. */
    public Jugador buscar(String id) {
        for (NodoCubeta n = cubetas[cubetaDe(id)]; n != null; n = n.sig) {
            if (n.jugador.id.equals(id)) {
                return n.jugador;
            }
        }
        return null;
    }

    /** Igual que buscar, pero cuenta en voz alta como recorre la cubeta. */
    public Jugador buscarConTraza(String id) {
        int c = cubetaDe(id);
        System.out.println("Buscar " + id + ": suma " + sumaCodigos(id) + " → cubeta " + c);
        for (NodoCubeta n = cubetas[c]; n != null; n = n.sig) {
            if (n.jugador.id.equals(id)) {
                System.out.println("  recorre " + n.jugador.id + " " + n.jugador.nombre + ": encontrado");
                return n.jugador;
            }
            System.out.println("  recorre " + n.jugador.id + " " + n.jugador.nombre + ": no es");
        }
        System.out.println("  AVISO: el jugador " + id + " no existe en la tabla.");
        return null;
    }

    public void imprimir() {
        for (int c = 0; c < CUBETAS; c++) {
            StringBuilder sb = new StringBuilder("  cubeta " + c + ": ");
            if (cubetas[c] == null) {
                sb.append("(vacía)");
            }
            for (NodoCubeta n = cubetas[c]; n != null; n = n.sig) {
                sb.append(n.jugador.id).append(" ").append(n.jugador.nombre)
                  .append(" (suma ").append(sumaCodigos(n.jugador.id)).append(")");
                if (n.sig != null) {
                    sb.append(" → ");
                }
            }
            System.out.println(sb);
        }
    }
}
