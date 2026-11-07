# Libro MX 1.2.0 (Forge 1.20.1) - con recompensas

## Compilar en GitHub
1. Crea un repositorio y sube TODO el contenido de esta carpeta (incluida `.github/workflows/build.yml`).
2. Ve a **Actions > Compilar Libro MX** (corre solo al hacer push, o con "Run workflow").
3. Al terminar, descarga el artefacto **libromx-jar** y usa `libromx-1.2.0.jar` en tu carpeta `mods`.

## Uso
- Tecla **G**: abre el libro (Misiones / Notificaciones / Dificultad).
- Admin (op nivel 2): boton **Panel de misiones** dentro del libro.
  - Elige Matar / Obtener / Craftear, llena los objetivos y las **Recompensas** (cuadricula de abajo).
  - Clic en un espacio y luego en un objeto; clic derecho vacia; rueda cambia cantidad (Shift = x10).
  - Boton `>>` (arriba a la izquierda): lista de misiones para editar [E] o borrar [X].
- Al cumplir una mision el jugador recibe las recompensas solo (si no caben, caen al suelo).
  - Matar: cuenta mobs que mata el jugador. Craftear: cuenta lo crafteado. Obtener: se cumple al tener los objetos en el inventario.

## Comandos (op)
- `/libro` abre el libro
- `/libro mision agregar <tipo> <objetivo> "<titulo>" <descripcion>` (tipo: kill, collect, craft)
- `/libro mision recompensa <n> <item> [cantidad]`
- `/libro mision quitar <n>`
- `/libro notificacion enviar <texto>` / `quitar <n>`
- `/libro dificultad agregar <base> "<titulo>" <descripcion>` / `establecer <n>` / `quitar <n>`
- `/libro lista <mision|notificacion|dificultad>`
- `/libro reiniciar [todos]` reinicia el progreso (para probar recompensas)

Codigos de texto: `&a` colores, `&l` negrita, `&#RRGGBB`, emojis `:corazon: :estrella: :calavera: :diamante: :chile: :cempasuchil:`.
Datos en el mundo: `libromx.json` (libro, compatible con el anterior) y `libromx_progress.json` (progreso).
