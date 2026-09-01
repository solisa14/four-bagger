package com.github.solisa14.fourbagger.api.config;

import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///fourbagger?TC_DAEMON=true",
            "spring.datasource.hikari.maximum-pool-size=2",
            "spring.datasource.hikari.minimum-idle=0",
            "spring.datasource.hikari.connection-timeout=10000",
            "logging.level.root=OFF"
        })
class DatabaseConnectionRecoveryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void databaseBackedRequest_afterTemporaryDatabaseUnavailability_recovers() throws Exception {
        RegisterUserRequest unavailableRequest = TestDataFactory.registerUserRequest();
        RegisterUserRequest recoveredRequest = TestDataFactory.registerUserRequest();
        String database;
        String adminUrl;
        try (Connection connection = dataSource.getConnection()) {
            database = connection.getCatalog();
            String url = connection.getMetaData().getURL();
            int queryStart = url.indexOf('?');
            String urlWithoutQuery = queryStart < 0 ? url : url.substring(0, queryStart);
            adminUrl = urlWithoutQuery.substring(0, urlWithoutQuery.lastIndexOf('/') + 1) + "postgres";
        }

        try (Connection adminConnection = DriverManager.getConnection(adminUrl, "test", "test")) {
            setConnectionsAllowed(adminConnection, database, false);
            terminateConnections(adminConnection, database);
            register(unavailableRequest).andExpect(status().is5xxServerError());
            setConnectionsAllowed(adminConnection, database, true);
            register(recoveredRequest).andExpect(status().isCreated());
        } finally {
            try (Connection adminConnection = DriverManager.getConnection(adminUrl, "test", "test")) {
                setConnectionsAllowed(adminConnection, database, true);
            }
        }
    }

    private ResultActions register(RegisterUserRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));
    }

    private void setConnectionsAllowed(Connection connection, String database, boolean allowed) throws SQLException {
        String escapedDatabase = database.replace("\"", "\"\"");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER DATABASE \"" + escapedDatabase + "\" WITH ALLOW_CONNECTIONS " + allowed);
        }
    }

    private void terminateConnections(Connection connection, String database) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = ?")) {
            statement.setString(1, database);
            statement.execute();
        }
    }
}
