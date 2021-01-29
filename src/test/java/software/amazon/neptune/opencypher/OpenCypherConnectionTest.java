/*
 * Copyright <2020> Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package software.amazon.neptune.opencypher;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.jdbc.utilities.Logging;
import software.amazon.neptune.NeptuneConstants;
import software.amazon.neptune.opencypher.mock.MockOpenCypherDatabase;
import software.amazon.neptune.opencypher.mock.MockOpenCypherNodes;
import software.amazon.neptune.opencypher.resultset.OpenCypherResultSet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class OpenCypherConnectionTest {
    private static final String HOSTNAME = "localhost";
    private static final String QUERY =
            "MATCH (p1:Person)-[:KNOWS]->(p2:Person)-[:GIVES_PETS_TO]->(k:Kitty) WHERE k.name = 'tootsie' RETURN p1, p2, k";
    private static final Properties PROPERTIES = new Properties();
    private static MockOpenCypherDatabase database;
    private java.sql.Connection connection;

    /**
     * Function to get a random available port and initialize database before testing.
     */
    @BeforeAll
    public static void initializeDatabase() {
        database = MockOpenCypherDatabase.builder(HOSTNAME, OpenCypherConnectionTest.class.getName())
                .withNode(MockOpenCypherNodes.LYNDON)
                .withNode(MockOpenCypherNodes.VALENTINA)
                .withNode(MockOpenCypherNodes.VINNY)
                .withNode(MockOpenCypherNodes.TOOTSIE)
                .withRelationship(MockOpenCypherNodes.LYNDON, MockOpenCypherNodes.VALENTINA, "KNOWS", "KNOWS")
                .withRelationship(MockOpenCypherNodes.LYNDON, MockOpenCypherNodes.VINNY, "GIVES_PETS_TO",
                        "GETS_PETS_FROM")
                .withRelationship(MockOpenCypherNodes.VALENTINA, MockOpenCypherNodes.TOOTSIE, "GIVES_PETS_TO",
                        "GETS_PETS_FROM")
                .build();
        PROPERTIES.putIfAbsent(NeptuneConstants.ENDPOINT,
                String.format("bolt://%s:%d", HOSTNAME, database.getPort()));
    }


    /**
     * Function to get a shutdown database after testing.
     */
    @AfterAll
    public static void shutdownDatabase() {
        database.shutdown();
    }

    @BeforeEach
    void initialize() throws SQLException {
        connection = new OpenCypherConnection(PROPERTIES);
    }

    @Test
    void testOpenCypherConnectionPrepareStatementType() {
        final AtomicReference<PreparedStatement> statement = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> statement.set(connection.prepareStatement(QUERY)));
        Assertions.assertTrue(statement.get() instanceof OpenCypherPreparedStatement);

        final AtomicReference<ResultSet> openCypherResultSet = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> openCypherResultSet.set(statement.get().executeQuery()));
        Assertions.assertTrue(openCypherResultSet.get() instanceof OpenCypherResultSet);
    }

    @Test
    void testOpenCypherConnectionStatementType() {
        final AtomicReference<Statement> statement = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> statement.set(connection.createStatement()));
        Assertions.assertTrue(statement.get() instanceof OpenCypherStatement);

        final AtomicReference<ResultSet> openCypherResultSet = new AtomicReference<>();
        Assertions.assertDoesNotThrow(() -> openCypherResultSet.set(statement.get().executeQuery(QUERY)));
        Assertions.assertTrue(openCypherResultSet.get() instanceof OpenCypherResultSet);
    }

    @Test
    void testLogLevelChanged() throws SQLException {
        Assertions.assertEquals(Logging.DEFAULT_LEVEL, LogManager.getRootLogger().getLevel());

        final Properties properties = new Properties();
        properties.putAll(PROPERTIES);
        properties.put("logLevel", "ERROR");
        connection = new OpenCypherConnection(properties);
        Assertions.assertEquals(Level.ERROR, LogManager.getRootLogger().getLevel());

        // Reset logging so that it doesn't affect other tests.
        LogManager.getRootLogger().setLevel(Logging.DEFAULT_LEVEL);
    }
}
