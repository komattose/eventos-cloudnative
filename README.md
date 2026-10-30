# Eventos – Desarrollo Cloud Native (CDY2204)

Microservicio Spring Boot para el registro y la venta de entradas de una empresa de eventos.
Experiencia 1 – Semana 1: *Desplegando aplicaciones en la nube*. Grupo 20.

## Requerimientos y cómo se cumplen

| Requerimiento | Implementación |
|---|---|
| Endpoint que lista los eventos disponibles (nombre, fecha, precio) | `GET /api/eventos` |
| Endpoint para agregar eventos con persistencia en Oracle Cloud | `POST /api/eventos` → tabla `EVENTOS` |
| Endpoint de compra de entradas que devuelve un resumen (evento, nº de entradas, precio unitario y total) y lo guarda en Oracle Cloud | `POST /api/compras` → tabla `COMPRAS` |
| Despliegue automático al hacer push a `main`: imagen en Docker Hub y despliegue en EC2 | `.github/workflows/ci-cd.yml` |

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/eventos` | Lista los eventos con fecha de hoy en adelante |
| GET | `/api/eventos/{id}` | Detalle de un evento |
| POST | `/api/eventos` | Crea un evento |
| POST | `/api/compras` | Compra entradas y devuelve el resumen de compra |
| GET | `/api/compras/{numeroResumen}` | Consulta un resumen de compra |
| GET | `/actuator/health` | Estado de la aplicación |

Ejemplo de compra:

```json
POST /api/compras
{ "eventoId": 1, "cantidadEntradas": 3 }
```

```json
201 Created
{
  "numeroResumen": 1,
  "eventoId": 1,
  "evento": "Concierto Rock en el Parque",
  "fechaEvento": "2026-12-15",
  "cantidadEntradas": 3,
  "precioUnitario": 25000.00,
  "precioTotal": 75000.00,
  "fechaCompra": "2026-10-30T12:00:00"
}
```

Los errores se devuelven en formato `ProblemDetail`: 400 (datos inválidos), 404 (evento o resumen inexistente) y 409 (evento ya realizado).

## Arquitectura

```
Controller (REST) → Service (reglas de negocio) → Repository (Spring Data JPA) → Oracle Cloud
```

- `model`: entidades `Evento` y `Compra`. El precio unitario se guarda en la compra para que el resumen no cambie si después se modifica el precio del evento.
- `dto`: objetos de entrada/salida con validaciones (`jakarta.validation`).
- `exception`: manejo centralizado de errores.

## Ejecutar en local (sin Oracle)

Requiere JDK 17 y Maven.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

El perfil `local` usa una base H2 en memoria. Las pruebas automáticas (`mvn test`) también la usan.

## Ejecutar contra Oracle Cloud

1. En Oracle Cloud, descarga la **wallet** de la Autonomous Database y descomprímela en una carpeta (por ejemplo `wallet/`, ignorada por Git).
2. Define las variables de entorno:

```bash
DB_URL=jdbc:oracle:thin:@<nombre_bd>_high?TNS_ADMIN=/ruta/a/wallet
DB_USERNAME=ADMIN
DB_PASSWORD=<contraseña>
```

3. `mvn spring-boot:run`

Las tablas se crean solas al iniciar (`ddl-auto=update`).

## CI/CD (GitHub Actions)

Cada push a `main` ejecuta tres etapas:

1. **build-test**: compila y ejecuta las pruebas automáticas (`mvn verify`).
2. **docker**: construye la imagen con el `Dockerfile` y la publica en Docker Hub (`<usuario>/eventos:latest` y `:<sha>`).
3. **deploy**: entra por SSH a la EC2, descarga la imagen nueva, reinicia el contenedor y verifica `/actuator/health`.

### Secrets del repositorio (Settings → Secrets and variables → Actions)

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | Access token de Docker Hub |
| `EC2_HOST` | IP pública o DNS de la instancia |
| `EC2_USER` | `ec2-user` (Amazon Linux) o `ubuntu` |
| `EC2_SSH_KEY` | Contenido de la llave privada `.pem` |
| `DB_URL` | `jdbc:oracle:thin:@<nombre_bd>_high?TNS_ADMIN=/app/wallet` |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |

### Preparar la instancia EC2 (una sola vez)

1. Instalar Docker y permitir que el usuario lo use (`sudo usermod -aG docker $USER`).
2. Copiar la wallet descomprimida a `~/wallet` en la instancia.
3. Abrir el puerto **8080** en el Security Group.

## Pruebas con Postman

Importa `postman/Eventos.postman_collection.json` y los entornos `postman/EC2.postman_environment.json` (API desplegada en `http://3.215.75.56:8080`) y `postman/Local.postman_environment.json`. Selecciona el entorno arriba a la derecha y ejecuta la colección con **Run collection**. Las peticiones guardan el `eventoId` y el `numeroResumen` automáticamente y validan las respuestas.
