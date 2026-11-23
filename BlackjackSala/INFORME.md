# Parcial de Estructuras de Datos — Sala de blackjack

**Integrantes:** _(escribir nombres)_
**Fecha:** 23 de noviembre de 2026

---

## 1. Qué hace el programa

Programa de consola en Java que pone en marcha una mesa de blackjack con crupier, zapato, manos, apuestas y una sala pequeña por la que el jugador llega a su asiento. La corrida de verificación usa cartas fijas (`java Main fija`), de modo que la salida es siempre la misma. Existe además un modo libre con el mazo barajado (`java Main libre`) que no reemplaza la corrida fija.

## 2. Dónde entra cada estructura

| Estructura | Uso en el juego |
|---|---|
| Cola (lista simple) | El zapato. `desencolar` suelta la carta por el frente y la siguiente queda como nuevo frente. |
| Pila (lista simple) | El descarte (la última carta repartida queda en la cima) y el historial de acciones (`deshacer` retira solo la cima). |
| Lista doble | La mano de cada jugador (la carta nueva se cuelga al final) y los asientos de la mesa. Ambas se leen en los dos sentidos. |
| Tabla hash | Jugadores por identificador, 5 cubetas, cubeta = suma de códigos del id módulo 5. J01 (171) y J06 (176) comparten la cubeta 1; J02 (172) cae en la 2. |
| Árbol binario de búsqueda | Ranking por fichas al cerrar la corrida (inserción: Marta, Ana, Luis). |
| Árbol binario de decisión | Cada mano baja por sí/no hasta una acción. Sirve para el jugador y para el crupier. |
| Grafo (listas de adyacencia) | Dijkstra hasta la mesa; Prim y Kruskal para conectar la sala. |

El zapato y el descarte son clases distintas (`Cola` y `Pila`) y no comparten código: una saca por el frente y la otra recibe y entrega por la cima. Ninguna usa `Queue`, `ArrayDeque`, `Stack`, `LinkedList`, `HashMap` ni `TreeMap`. La baraja de 52 cartas es un arreglo que solo se usa para construir el zapato.

## 3. Decisiones de la mesa

Estas son las decisiones que se tomaron donde el enunciado deja margen. Conviene poder explicarlas en la sustentación.

1. **Orden de reparto y de turnos.** En la ronda 1 se empieza por el primer asiento (Ana, Luis). En la ronda 2 el enunciado dice que el reparto es "Marta, Ana y el crupier". Para que eso sea compatible con "de izquierda a derecha", el asiento inicial rota una posición por ronda: en la ronda 2 empieza Luis, que no apuesta, así que sigue Marta y después Ana. Solo entran quienes apostaron.
2. **Revisión de la carta oculta.** Si la carta visible del crupier es un as o vale 10, se mira la oculta antes de que jueguen los jugadores. Si hay blackjack del crupier, el jugador con blackjack empata y los demás pierden la apuesta; nadie juega su turno.
3. **Cuándo se cobra.** El blackjack se paga 3 a 2 en el turno del jugador (el crupier ya no puede tener blackjack en ese punto). El que se pasa pierde en ese momento. Los demás se comparan con el crupier cuando este termina.
4. **Cuándo juega el crupier.** Juega si queda al menos un jugador que no se pasó, incluido uno con blackjack. Por eso en la ronda 1 pide aunque Luis ya se pasó: Ana sigue en juego. Ana no devuelve lo cobrado, porque su blackjack se resolvió contra un crupier sin blackjack.
5. **Ases.** La mano se repasa completa cada vez que entra una carta: los ases valen 11 y, mientras se pase de 21, uno por uno pasan a valer 1. El crupier se planta con 17 o más, sea 17 duro o con as.
6. **Descarte.** Una carta pasa a la pila de descarte cuando se cierra su jugada, es decir, apenas queda en la mano. Por eso la cima final es el 3 de picas, la última que salió del zapato.
7. **Historial.** Solo se apilan acciones (pedir, plantarse, pasarse, blackjack, y las del crupier), no los resultados. Así la última acción de la partida es la planta del crupier en 17, y al deshacer queda la planta de Ana en 16.
8. **Ranking.** En el árbol de búsqueda los valores menores van a la izquierda; los iguales, a la derecha. Peso = nodos sin contar la raíz; altura de una sola raíz = 0; grado = número de hijos; nivel = aristas desde la raíz; completo = cada nodo tiene cero hijos o dos.
9. **Empates en Dijkstra.** La relajación usa `<` estricto, así que en un empate se conserva el camino ya encontrado. Con T1 llena, R→T2 directo (6 min) empata con R→T1→T2 (4+2 = 6) y el programa informa R → T2, como pide la guía.

## 4. Corrida de verificación (resumen)

La transcripción completa está en `transcripcion_corrida_fija.txt`. Resumen de la salida:

```
Sala T1 libre. Dijkstra: R → T1, 4 min
Prim y Kruskal, costo 10. Rechazan T1—T3 (5), R—T2 (6) y T2—T3 (8)
T1 llena: la mesa libre más cercana es T2, R → T2, 6 min
Asientos Ana ↔ Luis ↔ Marta  /  al revés: Marta ↔ Luis ↔ Ana
Tabla: cubeta 1: J01 Ana y J06 Marta. J02 Luis en la cubeta 2

Ronda 1  Ana: As de picas, Rey de tréboles. Blackjack. Cobra 3 a 2. Fichas 650
         Luis: 9, 7 y 8. 24. Se pasa. Fichas 350
         Crupier: Rey, 6 y 5. 21. Se planta
         Mano de Luis al revés: 8 de diamantes, 7 de picas, 9 de corazones
Ronda 2  Marta: 10 y 7. 17. Se planta. Empata. Fichas 300
         Ana: 8, 5 y 3. 16. Se planta. Pierde. Fichas 550
         Crupier: 9 y 8. 17. Se planta

Descarte cima: 3 de picas
Historial cima: el crupier se planta en 17; al retirarla, la cima es la planta de Ana en 16
Ranking inorden 300, 350, 550. Completo: no. Peso: 2
350 por el camino 300 → 550 → 350, nivel 2, grado 0
```

Casos de borde, también parte de la corrida: buscar J99 avisa que el jugador no existe; la mesa T9 no produce camino; pedir carta con el zapato vacío avisa y no reparte una carta inventada.

## 5. Comentario: el camino hasta una mesa frente al árbol que conecta toda la sala

_(Texto de partida; conviene reescribirlo con las propias palabras.)_

Dijkstra y Prim/Kruskal trabajan sobre el mismo grafo pero responden preguntas distintas.

Dijkstra responde **desde un punto de partida**: cuál es el camino más corto desde la recepción hasta una mesa concreta. Lo que minimiza es la distancia acumulada desde R. Con T1 libre, T3 queda a 9 minutos por R → T1 → T3, porque 4 + 5 = 9 gana a R → T2 → T3, que cuesta 6 + 8 = 14. Ese camino usa la arista T1—T3.

Prim y Kruskal responden **sobre toda la sala**: qué conjunto de aristas deja todas las mesas conectadas con el menor costo total, como si se tendiera el cableado más barato. Lo que minimiza es la suma de las aristas del árbol, sin importar cuánto tarde alguien en caminar desde R hasta cada nodo. En esta sala ese árbol cuesta 10 y usa R—T1, T1—T2, T2—T4 y T3—T4; **rechaza T1—T3**, justo la arista que Dijkstra necesita para llegar a T3.

Por eso las dos soluciones no tienen por qué coincidir. En el cableado, llegar a T3 pasa por T4 (1 minuto, la arista más barata del grafo), y cerrar el ciclo con T1—T3 (5) sería gastar de más. Para una persona que camina, ese rodeo (R → T1 → T2 → T4 → T3 = 10 minutos) es peor que ir por T1—T3 (9 minutos). El árbol de cobertura mínima optimiza un costo global y no garantiza caminos cortos entre pares de nodos; Dijkstra optimiza cada trayecto desde R y puede usar aristas que un árbol de cobertura mínima descartaría. Además, un árbol de cobertura tiene siempre V−1 aristas y ningún ciclo, mientras que el conjunto de caminos mínimos desde R puede usar aristas distintas según el destino.

Prim y Kruskal llegan al mismo conjunto de aristas (en esta sala todos los pesos son distintos, así que el árbol es único), pero por caminos distintos: Prim crece desde R agregando cada vez la arista más barata que sale del árbol actual; Kruskal ordena todas las aristas por peso y acepta cada una que no cierre un ciclo. Por eso el orden en que aparecen las aristas es diferente, aunque el costo, 10, es el mismo.

## 6. Archivos entregados

`src/` con 14 clases Java, `README.md`, `transcripcion_corrida_fija.txt` y este informe.
