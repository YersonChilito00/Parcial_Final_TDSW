/**
 * Una carta de la baraja francesa. El valor base del as es 11; el ajuste a 1
 * lo hace la mano cada vez que se repasa.
 */
public class Carta {
    private static final String[] RANGOS = {
        "", "As", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jota", "Reina", "Rey"
    };
    public static final String[] PALOS = {"picas", "corazones", "diamantes", "tréboles"};

    private final String rango;
    private final String palo;
    private final int valorBase;

    /** rango de 1 (as) a 13 (rey); palo de 0 a 3. */
    public Carta(int rango, int palo) {
        this.rango = RANGOS[rango];
        this.palo = PALOS[palo];
        if (rango == 1) {
            this.valorBase = 11;
        } else if (rango >= 10) {
            this.valorBase = 10;
        } else {
            this.valorBase = rango;
        }
    }

    public String getRango() {
        return rango;
    }

    public String getPalo() {
        return palo;
    }

    public int getValorBase() {
        return valorBase;
    }

    public boolean esAs() {
        return valorBase == 11;
    }

    public boolean es(String rango, String palo) {
        return this.rango.equals(rango) && this.palo.equals(palo);
    }

    @Override
    public String toString() {
        return rango + " de " + palo;
    }
}
