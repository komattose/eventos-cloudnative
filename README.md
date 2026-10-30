# Eventos – Desarrollo Cloud Native (CDY2204)

Microservicio Spring Boot para el registro y la venta de entradas de una empresa de eventos.
Experiencia 1 – Semana 1: *Desplegando aplicaciones en la nube* y Semana 2: *Almacenando archivos en la nube*. Grupo 20.

## Requerimientos de la semana 2 (AWS S3)

| Requerimiento | Implementación |
|---|---|
| Generar el resumen de compra como archivo físico que se pueda guardar en el computador | `GET /api/compras/{n}/resumen/pdf` → descarga `resumen-compra-{n}.pdf` |
| Subir el resumen a un bucket S3, en una carpeta con el número del resumen | `POST /api/compras/{n}/resumen/s3` → objeto `{n}/resumen-compra-{n}.pdf` |
| Modificar el archivo en caso de error | `PUT /api/compras/{n}/resumen/s3` (PDF corregido en el campo `archivo`, o se regenera si no se envía) |
| Descargar el archivo | `GET /api/compras/{n}/resumen/s3` |
| Borrar el archivo | `DELETE /api/compras/{n}/resumen/s3` (valida que exista; si no, 404) |

Endpoints de apoyo: `GET /api/compras/{n}/resumen/s3/info` (bucket, carpeta, clave, tamaño y fecha) y `GET /api/compras/{n}/resumen/s3/carpeta` (objetos dentro de la carpeta del resumen).

Capas: `StorageConfig` (cliente S3 y credenciales) → `S3Repository` (operaciones sobre el bucket) → `ResumenS3Service` (reglas: carpeta por número de resumen, validar existencia, solo PDF) → `ResumenArchivoController`. `ResumenPdfService` genera el PDF con OpenPDF.

Variables de entorno: `S3_BUCKET`, `AWS_REGION` (por defecto `us-east-1`) y las credenciales de AWS Academy `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`. Si las credenciales no se entregan, se usa el rol de la instancia EC2 (LabRole).

## Requerimientos de la semana 1

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
| GET | `/api/compras/{n}/resumen/pdf` | Genera el resumen en PDF para descargarlo |
| POST | `/api/compras/{n}/resumen/s3` | Sube el resumen a S3 en la carpeta `{n}/` |
| PUT | `/api/compras/{n}/resumen/s3` | Modifica el archivo en S3 |
| GET | `/api/compras/{n}/resumen/s3` | Descarga el archivo desde S3 |
| DELETE | `/api/compras/{n}/resumen/s3` | Borra el archivo de S3 |
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
| `S3_BUCKET` | Nombre del bucket S3 donde se guardan los resúmenes |
| `AWS_ACCESS_KEY_ID` | AWS Academy → *AWS Details* (cambia en cada sesión del laboratorio) |
| `AWS_SECRET_ACCESS_KEY` | AWS Academy → *AWS Details* |
| `AWS_SESSION_TOKEN` | AWS Academy → *AWS Details* |
| `AWS_REGION` | Opcional, por defecto `us-east-1` |

### Preparar la instancia EC2 (una sola vez)

1. Instalar Docker y permitir que el usuario lo use (`sudo usermod -aG docker $USER`).
2. Copiar la wallet descomprimida a `~/wallet` en la instancia.
3. Abrir el puerto **8080** en el Security Group.

## Pruebas con Postman

Importa `postman/Eventos.postman_collection.json` y los entornos `postman/EC2.postman_environment.json` (API desplegada en `http://3.215.75.56:8080`) y `postman/Local.postman_environment.json`. Selecciona el entorno arriba a la derecha y ejecuta la colección con **Run collection**. Las peticiones guardan el `eventoId` y el `numeroResumen` automáticamente y validan las respuestas.
