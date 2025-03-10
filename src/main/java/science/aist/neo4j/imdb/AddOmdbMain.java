package science.aist.neo4j.imdb;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AddOmdbMain {

    public static void main(String[] args) {
        try (ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("neo4j-config.xml")) {
            appContext.getBean("importer", Importer.class).addOmdb(50000, 99);
        }
    }
}
