package cl.duoc.eventos.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Almacenamiento temporal de archivos en Amazon EFS.
 * En la EC2 el EFS está montado en /mnt/efs y se monta dentro del contenedor en la ruta de efs.path.
 * Para el EFS es una carpeta más del sistema de archivos, por eso se usa java.nio.
 */
@Repository
public class EfsRepository {

    private final Path raiz;

    public EfsRepository(@Value("${efs.path}") String ruta) {
        this.raiz = Path.of(ruta).toAbsolutePath().normalize();
    }

    public void guardar(String rutaRelativa, byte[] contenido) {
        Path archivo = resolver(rutaRelativa);
        try {
            Files.createDirectories(archivo.getParent());
            Files.write(archivo, contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en el EFS: " + rutaRelativa, e);
        }
    }

    public Optional<byte[]> leer(String rutaRelativa) {
        Path archivo = resolver(rutaRelativa);
        if (!Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(archivo));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer del EFS: " + rutaRelativa, e);
        }
    }

    public boolean eliminar(String rutaRelativa) {
        try {
            return Files.deleteIfExists(resolver(rutaRelativa));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar del EFS: " + rutaRelativa, e);
        }
    }

    public String rutaCompleta(String rutaRelativa) {
        return resolver(rutaRelativa).toString();
    }

    private Path resolver(String rutaRelativa) {
        Path archivo = raiz.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta fuera del EFS: " + rutaRelativa);
        }
        return archivo;
    }
}
