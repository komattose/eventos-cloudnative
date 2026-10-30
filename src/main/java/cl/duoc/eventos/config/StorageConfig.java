package cl.duoc.eventos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Autenticación con AWS S3.
 * Si se entregan las credenciales de AWS Academy (access key, secret key y session token) se usan esas;
 * si no, se usa la cadena por defecto de AWS (por ejemplo, el rol LabRole asociado a la instancia EC2).
 */
@Configuration
public class StorageConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.credentials.access-key:}")
    private String accessKey;

    @Value("${aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${aws.credentials.session-token:}")
    private String sessionToken;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credenciales())
                .build();
    }

    private AwsCredentialsProvider credenciales() {
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            return DefaultCredentialsProvider.create();
        }
        if (StringUtils.hasText(sessionToken)) {
            return StaticCredentialsProvider.create(AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
