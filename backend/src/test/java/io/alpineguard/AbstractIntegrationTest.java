package io.alpineguard;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Boots the application against the in-memory H2 + H2GIS database with the bundled seed data. */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
