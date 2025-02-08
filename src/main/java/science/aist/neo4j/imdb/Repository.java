package science.aist.neo4j.imdb;

import java.util.List;
import java.util.Map;

/**
 * <p>Base Repository Interface</p>
 */
public interface Repository {

    /**
     * Executes a given cypher query on the database
     *
     * @param query the query to be executed
     */
    void query(String query);

    void batchUpdate(String query, List<Map<String, Object>> batch);
}
