# La ciencia detrás de Cinetosis

## El conflicto sensorial

El mareo por movimiento (**cinetosis**) es, según la teoría más aceptada, el resultado de un
**conflicto entre sistemas sensoriales**:

- El **sistema vestibular** (oído interno) detecta aceleraciones lineales y rotaciones: nota que
  el vehículo arranca, frena, sube, baja o gira.
- La **visión**, en cambio, cuando miras una pantalla dentro del coche, percibe una escena
  **estática**: el texto, el mapa o el vídeo no se mueven respecto a ti.

El cerebro recibe dos mensajes contradictorios —«nos movemos» / «estamos quietos»— y esa
discrepancia es la que desencadena las náuseas, el malestar y el sudor frío.

## La estrategia: dar a los ojos la información que falta

Si la pantalla pudiera **mostrar el movimiento real** del vehículo, la visión y el oído interno
volverían a estar de acuerdo y el conflicto se reduciría. Esa es la base de la función de
accesibilidad de Apple y de **Cinetosis**:

> Mostrar **puntos animados en los bordes** de la pantalla que se desplazan en **tiempo real**
> reflejando las aceleraciones del vehículo.

### Mapa de aceleración → movimiento de los puntos

Los puntos se mueven de forma **coherente con lo que siente el cuerpo**:

| Situación del vehículo | Lo que siente el oído interno | Movimiento de los puntos |
|------------------------|-------------------------------|--------------------------|
| Acelera (arranca)      | Empuje hacia atrás            | Los puntos van hacia **atrás** (abajo) |
| Frena                  | Empuje hacia delante          | Los puntos van hacia **delante** (arriba) |
| Gira a la derecha      | Empuje hacia la izquierda     | Los puntos van hacia la **izquierda** |
| Gira a la izquierda    | Empuje hacia la derecha       | Los puntos van hacia la **derecha** |

La clave es que el desplazamiento visual sea **proporcional y simultáneo** a la aceleración
real, sin retardo perceptible.

## De los sensores al movimiento

Cinetosis obtiene el movimiento del teléfono combinando dos sensores:

1. **Acelerómetro lineal** (`TYPE_LINEAR_ACCELERATION`): aceleración del dispositivo **sin** el
   componente de la gravedad. Da el empuje longitudinal (acelerar/frenar) y lateral (curvas).
2. **Giroscopio** (`TYPE_GYROSCOPE`): velocidad angular. Ayuda a detectar giros y a estabilizar
   la señal.

Sobre estas señales se aplica **procesado**:

- **Filtro paso-bajo** para eliminar las vibraciones de alta frecuencia (baches, motor) que no
  corresponden al movimiento real del trayecto.
- **Suavizado temporal** para que los puntos se muevan de forma fluida y no «tiemblen».
- **Zona muerta** para ignorar microaceleraciones cuando el coche va estable.
- **Normalización por sensibilidad** configurable por el usuario.

## Por qué en los bordes y por qué puntos

- En los **bordes**: para que el indicador esté en la **visión periférica**, que es la más
  sensible al movimiento, sin tapar el contenido que estás mirando (mapa, libro, etc.).
- **Puntos pequeños y discretos**: suficientes para que el cerebro registre el movimiento, pero
  poco intrusivos.

## Limitaciones honestas

- La eficacia **varía** entre personas; para algunas es muy útil y para otras menos.
- No sustituye a recomendaciones clásicas (mirar al horizonte, ventilación, descansos).
- **No es un dispositivo médico**. Es una ayuda visual basada en una hipótesis sensorial bien
  fundamentada, pero no garantizada para todos los casos.

## Referencias y lecturas

- Teoría del conflicto sensorial de la cinetosis (Reason & Brand, *Motion Sickness*, 1975).
- Función «Vehicle Motion Cues / Indicadores de movimiento del vehículo» de Apple (iOS 18).
