# Eventos – Desarrollo Cloud Native (CDY2204)

Microservicio Spring Boot para el registro y la venta de entradas de una empresa de eventos.
Experiencia 1 – Semana 1: *Desplegando aplicaciones en la nube*, Semana 2: *Almacenando archivos en la nube* y Semana 3 (Sumativa 1): *Construyendo y desplegando una solución Cloud Native*. Grupo 20.

## Semana 3 – Sistema de Gestión de Reservas de Eventos con Tickets (EFS + S3)

| Requerimiento | Implementación |
|---|---|
| EFS para almacenamiento temporal: los tickets personalizados se generan y guardan en el EFS del microservicio | `EfsRepository` escribe en `EFS_PATH` (`/app/efs` en el contenedor = `/mnt/efs/tickets` en la EC2, donde está montado el EFS) |
| Subida automática a S3 por evento y usuario (`/evento123/usuarioX/ticket001.pdf`) | Al generar, cada ticket se sube a `evento{eventoId}/{usuario}/ticket{id}.pdf` |
| Generar tickets | `POST /api/tickets` (reserva + un ticket por entrada) |
| Subir tickets a S3 | `POST /api/tickets/{id}/s3` (uno) y `POST /api/tickets/s3?eventoId=&usuario=` (pendientes) |
| Descargar tickets por usuario o evento | `GET /api/tickets/{id}/descargar` (PDF) y `GET /api/tickets/descargar?eventoId=&usuario=` (ZIP) |
| Modificar detalles del ticket | `PUT /api/tickets/{id}` (regenera el PDF en EFS y actualiza el archivo en S3) |
| Eliminar tickets específicos | `DELETE /api/tickets/{id}` (borra de S3 y del EFS; el ticket queda `ELIMINADO`) |
| Estadísticas de ventas y reservas por evento | `GET /api/estadisticas/eventos/{eventoId}` y `GET /api/estadisticas` |
| Historial de archivos generados (pauta) | `GET /api/tickets/historial?eventoId=&usuario=&ticketId=&accion=&desde=&hasta=` |
| Imagen en Docker Hub y despliegue en EC2 con GitHub Actions al hacer push a `main` | `.github/workflows/ci-cd.yml` (monta el EFS en el contenedor) |

Apoyo: `GET /api/tickets?eventoId=&usuario=&estado=` (listar), `GET /api/tickets/{id}` y `GET /api/tickets/archivos-s3?eventoId=&usuario=` (objetos reales del bucket).

Flujo de un ticket: `GENERADO` (PDF en EFS) → `SUBIDO` (también en S3) → `ELIMINADO`. Si S3 no responde al generar, el ticket queda `GENERADO` en el EFS y se sube después con `POST /api/tickets/s3`. Cada operación (generado en EFS, subido a S3, modificado, descargado, eliminado) se registra en la tabla `HISTORIAL_ARCHIVOS`.

Ejemplo:

```json
POST /api/tickets
{ "eventoId": 4, "usuario": "martin", "cantidad": 2, "nombreAsistente": "Martin Belaunde", "zona": "vip" }
```

### Montar el EFS en la EC2 (una sola vez)

1. AWS → EFS → *Create file system* en la VPC por defecto.
2. En *Network* → *Manage*, asignar a cada zona el security group de la EC2, y en ese security group permitir **NFS (2049)** desde el mismo grupo.
3. En la EC2:

```bash
sudo mkdir -p /mnt/efs
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport <fs-id>.efs.us-east-1.amazonaws.com:/ /mnt/efs
echo "<fs-id>.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0" | sudo tee -a /etc/fstab
df -h /mnt/efs
```

El pipeline crea `/mnt/efs/tickets` y lo monta en el contenedor (`-v /mnt/efs/tickets:/app/efs`). Para ver los tickets en el EFS: `ls -R /mnt/efs/tickets`.

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
| POST | `/api/tickets` | Reserva y genera tickets (EFS + S3) |
| GET | `/api/tickets` | Lista tickets (filtros `eventoId`, `usuario`, `estado`) |
| GET | `/api/tickets/{id}` | Detalle de un ticket |
| POST | `/api/tickets/{id}/s3` | Sube un ticket a S3 desde el EFS |
| POST | `/api/tickets/s3` | Sube los tickets pendientes (por evento/usuario) |
| GET | `/api/tickets/{id}/descargar` | Descarga el PDF desde S3 |
| GET | `/api/tickets/descargar` | ZIP con los tickets de un evento y/o usuario |
| PUT | `/api/tickets/{id}` | Modifica nombre del asistente y/o zona |
| DELETE | `/api/tickets/{id}` | Elimina un ticket (S3 y EFS) |
| GET | `/api/tickets/historial` | Historial de archivos con filtros |
| GET | `/api/tickets/archivos-s3` | Objetos del bucket por evento/usuario |
| GET | `/api/estadisticas` | Estadísticas de todos los eventos |
| GET | `/api/estadisticas/eventos/{id}` | Estadísticas de ventas y reservas de un evento |
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
