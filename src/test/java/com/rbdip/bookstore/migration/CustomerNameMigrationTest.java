package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.target=4")
class CustomerNameMigrationTest {

    private static final int WAIT_SECONDS = 10;
    private static final List<String> EXISTING_NAMES =
            List.of("Ivan Petrov", "Madonna", "Anna Maria Smith", "Ivan  Petrov", "Trailing ");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_migration")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void seedLegacyCustomers() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO products (name, price) VALUES (?, ?)", "Refactoring", 40);
        for (String name : EXISTING_NAMES) {
            jdbc.update("INSERT INTO customers (full_name, address) VALUES (?, ?)", name, "Test address");
        }
    }

    @BeforeEach
    void configureHttpTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        restTemplate.getRestTemplate().setRequestFactory(factory);
    }

    @Test
    void backfillPreservesNamesAndExistingCustomers() {
        assertThat(jdbcTemplate.queryForList("SELECT first_name FROM customers", String.class)).doesNotContainNull();
        assertThat(jdbcTemplate.queryForList("""
                SELECT CASE WHEN last_name IS NULL THEN first_name
                            ELSE first_name || ' ' || last_name END FROM customers
                """, String.class)).containsAll(EXISTING_NAMES);

        int customers = jdbcTemplate.queryForObject("SELECT count(*) FROM customers", Integer.class);
        for (String name : EXISTING_NAMES) {
            exerciseOrdersApi(name);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM customers", Integer.class))
                .as("Existing migrated customers must be reused").isEqualTo(customers);
    }

    @Test
    void contractMigrationKeepsOrdersApiAvailable() throws Exception {
        migrateUnderLoad("5");
        exerciseOrdersApi("After Migration");
        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns WHERE table_name = 'customers'
                """, String.class)).contains("first_name", "last_name").doesNotContain("full_name");
        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns WHERE table_name = 'orders'
                """, String.class)).doesNotContain("customer_full_name", "customer_address", "customer_phone");
    }

    private void migrateUnderLoad(String target) throws Exception {
        AtomicBoolean keepSending = new AtomicBoolean(true);
        CountDownLatch migrationStarted = new CountDownLatch(1);
        CountDownLatch requestCompleted = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var load = executor.submit(() -> {
                await(migrationStarted);
                while (keepSending.get()) {
                    exerciseOrdersApi("Load Customer");
                    requestCompleted.countDown();
                }
            });
            try {
                var result = Flyway.configure().dataSource(jdbcTemplate.getDataSource())
                        .target(target).callbacks(new BaseCallback() {
                            @Override
                            public void handle(Event event, Context context) {
                                if (event == Event.BEFORE_EACH_MIGRATE) {
                                    migrationStarted.countDown();
                                    await(requestCompleted);
                                }
                            }
                        }).load().migrate();
                assertThat(result.migrationsExecuted).isEqualTo(1);
            } finally {
                keepSending.set(false);
                migrationStarted.countDown();
                load.get(WAIT_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(WAIT_SECONDS, TimeUnit.SECONDS))
                    .as("Migration and HTTP load must run concurrently").isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void exerciseOrdersApi(String name) {
        CreateOrderRequest request = new CreateOrderRequest(name, "Test address", null,
                "regular", null, List.of(new CreateOrderRequest.Item(1L, 1)));
        var created = restTemplate.postForEntity("/orders", request, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("status", "new");
        var listed = restTemplate.getForEntity("/orders", List.class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).anySatisfy(value -> {
            Map<?, ?> order = (Map<?, ?>) value;
            assertThat(order.get("id").toString()).isEqualTo(created.getBody().get("id").toString());
            assertThat(order.get("customerFullName")).isEqualTo(name);
            assertThat((List<?>) order.get("items")).hasSize(1);
        });
    }
}
