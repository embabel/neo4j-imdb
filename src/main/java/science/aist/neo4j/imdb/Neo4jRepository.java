package science.aist.neo4j.imdb;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.util.List;
import java.util.Map;

/**
 * <p>Simple Repository to execute cypher queries</p>
 */
public class Neo4jRepository implements Repository {
    private final Driver driver;

    public Neo4jRepository(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void query(String query) {
        try(Session session = driver.session();
            Transaction transaction = session.beginTransaction()) {
            transaction.run(query);
            transaction.commit();
        }
    }

    @Override
    public void batchUpdate(String query, List<Map<String, Object>> batch) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, Map.of("batch", batch));
                return null;
            });
        }
    }
}
