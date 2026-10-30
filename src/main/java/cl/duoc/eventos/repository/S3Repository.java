package cl.duoc.eventos.repository;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los objetos de un bucket de AWS S3.
 */
@Repository
public class S3Repository {

    private final S3Client s3Client;

    public S3Repository(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void guardar(String bucket, String clave, byte[] contenido, String tipoContenido) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(clave)
                        .contentType(tipoContenido)
                        .build(),
                RequestBody.fromBytes(contenido));
    }

    public byte[] obtener(String bucket, String clave) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(clave).build()).asByteArray();
    }

    /**
     * Metadatos del objeto, o vacío si no existe en el bucket.
     */
    public Optional<HeadObjectResponse> metadatos(String bucket, String clave) {
        try {
            return Optional.of(s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(clave).build()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public List<S3Object> listar(String bucket, String carpeta) {
        return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(carpeta).build()).contents();
    }

    public void eliminar(String bucket, String clave) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
    }
}
