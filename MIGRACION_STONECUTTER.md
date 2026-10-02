# GALYX — migración a Stonecutter (1.21.8 + 1.21.11)

## Qué cambió en la estructura

```
galyx/
├─ settings.gradle.kts          (registra Stonecutter y las versiones 1.21.8 / 1.21.11)
├─ stonecutter.gradle.kts       (controlador: versión activa en el IDE + tareas)
├─ stonecutter.properties.toml  (mod.version, mod.id, y las props por versión: fabric_api, etc.)
├─ build.gradle.kts             (tu build.gradle de siempre, ahora Kotlin + Stonecutter-aware)
├─ gradle.properties            (solo opciones de Gradle, ya no props del mod)
├─ src/
│   ├─ main/...                 (TU código, sin cambios — se comparte entre ambas versiones)
│   └─ client/...
└─ versions/                    (se genera solo al sincronizar Gradle, NO lo toques a mano)
    ├─ 1.21.8/
    └─ 1.21.11/
```

Un solo `src/`, pero dos jars distintos al compilar. No hay carpetas duplicadas de código.

## Flujo de trabajo día a día

1. **Elegir con qué versión trabajas en el IDE**: en IntelliJ (con el plugin de Stonecutter
   instalado) hay un botón para cambiar la "versión activa". Sin el plugin, corre:
   ```
   ./gradlew "1.21.8:project"   # o
   ./gradlew "1.21.11:project"
   ```
   Esto NO recompila nada solo, simplemente le dice a tu IDE contra qué mappings
   mostrarte el código (autocompletado, errores, etc.).

2. **Compilar UNA versión**:
   ```
   ./gradlew :1.21.8:build
   ./gradlew :1.21.11:build
   ```

3. **Compilar TODO de una vez** (genera ambos jars):
   ```
   ./gradlew chiseledBuild
   ```
   Los jars quedan en `build/libs/0.9.4-beta/1.21.8/` y `build/libs/0.9.4-beta/1.21.11/`.

4. **Probar en el juego**: sigue igual que siempre — corre el cliente de la versión activa
   con `./gradlew :1.21.8:runClient`, o instala el jar compilado vía CurseForge.

## Código que probablemente necesite diferenciarse por versión

Todavía no sabemos qué tan compatibles son las mappings de Mojang entre 1.21.8 y 1.21.11
hasta que compiles — son versiones cercanas, así que lo más probable es que la MAYORÍA
del código compile igual en ambas. Los sospechosos más probables si algo falla:

- `BossHealthOverlayMixin` y `GuiActionBarMixin` (mixins a clases vanilla de la GUI — estas
  clases son las que más cambian de nombre/firma entre versiones)
- Cualquier lugar donde leas `ItemStack` / data components para confirmar los drops
  (Cristal de Olympium, Fragmento de Zafiro)

Cuando el compilador se queje de algo que no existe en una de las dos versiones, se
resuelve así, directo en el `.java` compartido:

```java
//? 1.21.8 {
/*Código válido SOLO en 1.21.8*/
//?} else {
Código válido en 1.21.11 (y cualquier otra versión que agregues después)
//?}
```

Cuando cambias la versión activa (paso 1), Stonecutter comenta/descomenta estos bloques
automáticamente para que el IDE solo te muestre el código real de esa versión.

## Importante — no pude compilar esto por ti

Mi sandbox no tiene acceso a `maven.fabricmc.net` ni a los servidores de Mojang, así que
no pude correr `./gradlew build` para verificar que compile tal cual. La estructura y las
versiones de Fabric API (`0.136.1+1.21.8` y `0.141.6+1.21.11`) están verificadas contra
Modrinth, pero es muy posible que al compilar salte 1 o 2 errores puntuales de mapeo entre
1.21.8 y 1.21.11 — es normal en este tipo de migración y se resuelven con el patrón
`//? version { } else { }` de arriba.

## Próximo paso sugerido

1. Descomprime esto sobre tu proyecto actual (o ábrelo como proyecto nuevo).
2. `./gradlew build` (o sync en IntelliJ) y me pegas cualquier error de compilación —
   trabajamos los `//? {}` juntos con el código real.
3. Una vez compile limpio en ambas, seguimos con el release de la beta.
