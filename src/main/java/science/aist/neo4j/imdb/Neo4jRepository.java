package science.aist.neo4j.imdb;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.internal.value.NodeValue;

import java.util.ArrayList;
import java.util.HashMap;
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
        try (Session session = driver.session();
             Transaction transaction = session.beginTransaction()) {
            transaction.run(query);
            transaction.commit();
        }
    }

    @Override
    public void update(String query, Map<String, Object> params) {
        try (Session session = driver.session();
             Transaction transaction = session.beginTransaction()) {
            transaction.run(query, params);
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

    @Override
    public List<Map<String, Object>> queryForMap(String query, Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Session session = driver.session()) {
            var result = session.run(query, params);

            while (result.hasNext()) {
                var record = result.next();
                Map<String, Object> row = new HashMap<>();

                // Convert each field in the record to a map entry
                for (String key : record.keys()) {
                    Value value = record.get(key);

                    // Handle different Neo4j value types
                    Object convertedValue = convertNeo4jValue(value);
                    row.put(key, convertedValue);
                }

                results.add(row);
            }
        }

        return results;
    }

    private Object convertNeo4jValue(Value value) {
        if (value.isNull()) {
            return null;
        }

        // Handle different Neo4j types
        if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.INTEGER())) {
            return value.asLong();
        }
        if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.FLOAT())) {
            return value.asDouble();
        }
        if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.STRING())) {
            return value.asString();
        }
        if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.BOOLEAN())) {
            return value.asBoolean();
        }
        if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.LIST())) {
            List<Object> list = new ArrayList<>();
            for (var item : value.asList()) {
                list.add(convertNeo4jValue((Value) item));
            }
            return list;
        }
        if (value.hasType(InternalTypeSystem.TYPE_SYSTEM.MAP())) {
            Map<String, Object> map = new HashMap<>();
            for (var entry : value.asMap().entrySet()) {
                map.put(entry.getKey(), convertNeo4jValue(((Value) entry.getValue())));
            }
            return map;
        }
        if (value instanceof NodeValue) {
            return convertNodeToMap((NodeValue) value);
        }

        // Default fallback - convert to string
        return value.toString();
    }

    private Map<String, Object> convertNodeToMap(NodeValue nodeValue) {
        Map<String, Object> nodeMap = new HashMap<>();
        nodeMap.put("id", nodeValue.asNode().id());
        nodeMap.put("labels", nodeValue.asNode().labels());

        Map<String, Object> properties = new HashMap<>();
        nodeValue.asNode().asMap().forEach((key, value) ->
                properties.put(key, convertNeo4jValue(((Value) value)))
        );
        nodeMap.put("properties", properties);

        return nodeMap;
    }
}
