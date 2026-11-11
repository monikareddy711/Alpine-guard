package io.alpineguard.spatial;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class SpatialDialectConfig {

    @Bean
    public SpatialDialect spatialDialect(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (product.contains("postgresql")) {
                return new PostgisDialect();
            }
            return new H2GisDialect();
        }
    }
}
