/** Un jugador: identificador, nombre, fichas, apuesta de la ronda y su mano. */
public class Jugador {
    public final String id;
    public final String nombre;
    public int fichas;
    public int apuesta;
    public final Mano mano = new Mano();

    // Estado dentro de la ronda
    public boolean paso;
    public boolean blackjack;

    public Jugador(String id, String nombre, int fichas) {
        this.id = id;
        this.nombre = nombre;
        this.fichas = fichas;
    }

    /** Prepara al jugador para una ronda nueva (la apuesta ya se fijo antes). */
    public void nuevaRonda() {
        mano.limpiar();
        paso = false;
        blackjack = false;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
