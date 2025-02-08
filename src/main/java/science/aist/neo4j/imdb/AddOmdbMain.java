package science.aist.neo4j.imdb;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class AddOmdbMain {

    public static void main(String[] args) throws IOException {
        try (ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("neo4j-config.xml")) {
            appContext.getBean("importer", Importer.class).addOmdb(1000, 5000L);
        }
    }
}
