# Post-contenido — Unidad 12: Despliegue y CI/CD

**Estudiante:** Santiago Carreño Calceto
**Asignatura:** Programación Web — Séptimo Semestre
**Repositorio:** `carreno-post1-u12`

## Descripción

Repositorio del laboratorio de la Unidad 12. Contiene la aplicación Spring Boot **catálogo de productos** construida en la Unidad 10: una API REST (`/api/productos`) con la entidad JPA `Producto` y 25 pruebas. Aquí se dockeriza con un Dockerfile multi-stage y perfiles dev/prod (Parte 1). En la Parte 2 se extenderá con un pipeline de integración continua en GitHub Actions y monitoreo con Spring Boot Actuator.

## Parte 1 — Dockerización con Perfiles y Variables de Entorno

El **Dockerfile multi-stage** compila el proyecto con Maven en una etapa JDK (`maven:3.9.9-eclipse-temurin-21-alpine`) y ejecuta el JAR resultante en una etapa **JRE Alpine** (`eclipse-temurin:21-jre-alpine`) con un **usuario no root** (`spring`).

La configuración sensible se externaliza con placeholders `${VAR:default}`:

| Archivo | Contenido |
|---------|-----------|
| `application.properties` | Común: `server.port=${PORT:8080}`, `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}` |
| `application-dev.properties` | H2 en memoria (`DATASOURCE_URL`, `DATASOURCE_DRIVER`, `DATASOURCE_USER`, `DATASOURCE_PASSWORD` con defaults de desarrollo), `ddl-auto=create-drop`, SQL visible, log DEBUG |
| `application-prod.properties` | PostgreSQL (`DATASOURCE_URL` o `DB_HOST`/`DB_PORT`/`DB_NAME`, `DATASOURCE_USER`, `DATASOURCE_PASSWORD` sin default utilizable), `ddl-auto=${JPA_DDL_AUTO:validate}`, log WARN/INFO |

Sin variables de entorno, la aplicación arranca con el perfil **dev** (H2 en memoria). Con `SPRING_PROFILES_ACTIVE=prod` y las variables de la base de datos, arranca con la configuración de **producción**.

### Variables de entorno

| Variable | Perfil | Default | Uso |
|----------|--------|---------|-----|
| `SPRING_PROFILES_ACTIVE` | — | `dev` | Perfil activo |
| `PORT` | — | `8080` | Puerto HTTP (Render/Railway la inyectan) |
| `DATASOURCE_URL` | dev / prod | H2 en memoria / `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}` | URL JDBC |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | prod | `localhost`, `5432`, `appdb` | Alternativa a la URL completa |
| `DATASOURCE_USER` | dev / prod | `sa` / `appuser` | Usuario de la base |
| `DATASOURCE_PASSWORD` | dev / prod | vacío | Contraseña (en prod siempre por variable) |
| `JPA_DDL_AUTO` | prod | `validate` | `update` solo en el primer despliegue sobre una base vacía |

## Cómo compilar y ejecutar

```bash
# 1. Compilar y probar sin Docker
./mvnw clean package            # Windows: mvnw.cmd clean package
java -jar target/catalogo-0.0.1-SNAPSHOT.jar

# 2. Construir la imagen
docker build -t mi-spring-app:local .
docker images mi-spring-app      # tamaño < 300 MB

# 3. Ejecutar con los valores por defecto (perfil dev, H2 en memoria)
docker run -d --name mi-app-dev -p 8080:8080 mi-spring-app:local
curl "http://localhost:8080/api/productos?categoria=ELECTRONICA"
docker stop mi-app-dev && docker rm mi-app-dev

# 4. Ejecutar con perfil prod y variables de entorno explícitas
docker run -d --name mi-app-prod -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/appdb \
  -e DATASOURCE_USER=appuser \
  -e DATASOURCE_PASSWORD=apppass \
  -e JPA_DDL_AUTO=update \
  mi-spring-app:local
docker logs -f mi-app-prod       # debe mostrar: The following 1 profile is active: "prod"
```

## Decisiones de diseño — Parte 1

1. **Placeholders con valor por defecto (`${VAR:default}`) en vez de placeholders estrictos.** Un `${VAR}` sin default obliga a exportar todas las variables incluso en local o en CI, y falla con `PlaceholderResolutionException` si falta una. Con defaults razonables (H2 en memoria), el mismo artefacto arranca sin fricción en la máquina del desarrollador y en GitHub Actions, y en producción la plataforma sobrescribe cada valor. La excepción es `DATASOURCE_PASSWORD` en prod, que no tiene un default utilizable.
2. **`application-dev.properties` y `application-prod.properties` como archivos separados**, en vez de un único archivo con bloques `spring.config.activate.on-profile`. El nombre del archivo deja explícito qué configuración corresponde a cada ambiente y reduce el riesgo de editar por error una propiedad de producción.
3. **Imagen final `eclipse-temurin:21-jre-alpine` en vez de JDK completo o distroless.** Es mucho más liviana que el JDK, porque no trae compilador ni herramientas de build, y conserva un shell mínimo para depurar con `docker exec -it`, algo que distroless no permite.
4. **Etapa de compilación con la imagen oficial `maven:3.9.9-eclipse-temurin-21-alpine`.** La imagen `eclipse-temurin:21-jdk-alpine` no trae Maven, así que `RUN mvn ...` fallaría. `pom.xml` se copia antes que `src/` para que la capa de dependencias quede en caché.
5. **`ddl-auto=${JPA_DDL_AUTO:validate}` en prod.** Por defecto Hibernate solo valida el esquema y nunca lo modifica. Para el primer despliegue sobre una base vacía se pasa `JPA_DDL_AUTO=update`, sin cambiar el código.
