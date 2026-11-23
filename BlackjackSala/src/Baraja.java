import java.util.Random;

/**
 * La baraja de 52 cartas como ARREGLO. Solo sirve para construir el zapato:
 * a partir de ahi todo se reparte desde la Cola.
 */
public class Baraja {
    private final Carta[] cartas = new Carta[52];

    /** Cartas fijas de la corrida de verificacion, en orden de salida. */
    private static final String[][] ZAPATO_FIJO = {
        {"As", "picas"}, {"9", "corazones"}, {"Rey", "diamantes"}, {"Rey", "tréboles"},
        {"7", "picas"}, {"6", "corazones"}, {"8", "diamantes"}, {"5", "tréboles"},
        {"10", "picas"}, {"8", "tréboles"}, {"9", "picas"}, {"7", "diamantes"},
        {"5", "corazones"}, {"8", "corazones"}, {"3", "picas"}
    };

    public Baraja() {
        int i = 0;
        for (int palo = 0; palo < 4; palo++) {
            for (int rango = 1; rango <= 13; rango++) {
                cartas[i++] = new Carta(rango, palo);
            }
        }
    }

    /**
     * Zapato de la corrida fija: primero las 15 cartas de la guia, en ese
     * orden, y despues el resto de la baraja (sin repetir) en orden natural.
     */
    public Cola<Carta> zapatoDeCorrida() {
        Cola<Carta> zapato = new Cola<Carta>();
        boolean[] usada = new boolean[52];
        for (int k = 0; k < ZAPATO_FIJO.length; k++) {
            for (int i = 0; i < 52; i++) {
                if (!usada[i] && cartas[i].es(ZAPATO_FIJO[k][0], ZAPATO_FIJO[k][1])) {
                    usada[i] = true;
                    zapato.encolar(cartas[i]);
                    break;
                }
            }
        }
        for (int i = 0; i < 52; i++) {
            if (!usada[i]) {
                zapato.encolar(cartas[i]);
            }
        }
        return zapato;
    }

    /** Zapato barajado (modo libre). Baraja una copia con Fisher-Yates. */
    public Cola<Carta> zapatoBarajado(Random azar) {
        Carta[] copia = new Carta[52];
        for (int i = 0; i < 52; i++) {
            copia[i] = cartas[i];
        }
        for (int i = 51; i > 0; i--) {
            int j = azar.nextInt(i + 1);
            Carta tmp = copia[i];
            copia[i] = copia[j];
            copia[j] = tmp;
        }
        Cola<Carta> zapato = new Cola<Carta>();
        for (int i = 0; i < 52; i++) {
            zapato.encolar(copia[i]);
        }
        return zapato;
    }
}
