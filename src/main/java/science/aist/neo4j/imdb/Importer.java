package science.aist.neo4j.imdb;

import me.tongfei.progressbar.ProgressBar;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>The Neo4j Importer</p>
 */
public class Importer implements Runnable {
    private final Repository repository;

    private final TSV2CSV tsv2CSV;
    private final int maxRelations;

    public Importer(Repository repository, TSV2CSV tsv2CSV, int maxRelations) {
        this.repository = repository;
        this.tsv2CSV = tsv2CSV;
        this.maxRelations = maxRelations;
    }

    @Override
    public void run() {
        try {
            // Import Persons to database
            int cntRel = tsv2CSV.processRelations(maxRelations);
            int cntPerson = tsv2CSV.processPerson();
            int cntMovie = tsv2CSV.processMovies();

            // creates indices for person and movie ids to improve the query performance for the creation of the relations
            repository.query("create index person_id_index FOR (p:Person) ON (p.id)");
            repository.query("create index movie_id_index FOR (m:Movie) ON (m.id)");

            try (ProgressBar pb = new ProgressBar("Person", cntPerson)) {
                for (int i = 0; i <= cntPerson; i++) {
                    String statement = "LOAD CSV WITH HEADERS FROM 'file:///myData" + i + "person.csv' AS row\n " +
                            "CREATE (e:Person {id: row.id, name: row.name, dateOfBirth: toInteger(row.dob), dateOfDeath: toInteger(row.dod), primaryProfession: split(row.pp, \";\"), knownForTitles: split(row.kft, \";\")})";
                    repository.query(statement);
                    pb.step();
                }
            }

            // Import movies to database
            try (ProgressBar pb = new ProgressBar("Movies", cntMovie)) {
                for (int i = 0; i <= cntMovie; i++) {
                    String statement = "LOAD CSV WITH HEADERS FROM 'file:///myData" + i + "movie.csv' AS row\n " +
                            "CREATE (e:Movie {id: row.id, originalTitle: row.name, primaryTitle: row.primTitle, type: row.type, isAdult: toBoolean(row.isAdult), startYear: toInteger(row.startYear), endYear: toInteger(row.endYear), runtimeMinutes: toInteger(row.runtimeMinutes), genres: split(row.genres, \";\")})";
                    repository.query(statement);
                    pb.step();
                }
            }

            // Sometimes it seems that neo4j is creating the index when we do not query it once.
            repository.query("match(m: Movie {id: \"tt0000721\"}) return m");

            // Import relations
            try (ProgressBar pb = new ProgressBar("Relations", cntRel)) {
                for (int i = 0; i <= cntRel; i++) {
                    String statement = "LOAD CSV WITH HEADERS FROM 'file:///myData" + i + "rel.csv' AS row\n " +
                            "MATCH (m:Movie {id: row.titleid})\n " +
                            "MATCH (p:Person {id: row.personid})\n " +
                            "CREATE (p)-[r:part_of {category: row.category, job: row.job, characters: split(row.characters, \";\")}]->(m); ";
                    repository.query(statement);
                    pb.step();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addRatings(int minVotes) throws IOException {

        var classPathResource = new ClassPathResource("title.ratings.tsv");

        try (var br = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()))) {
            String line;
            List<Map<String, Object>> batch = new ArrayList<>();
            int batchSize = 1000; // Process 1000 movies at a time
            int linesRead = 0;
            while ((line = br.readLine()) != null) {
                linesRead++;
                if (linesRead == 1) {
                    // Skip the header
                    continue;
                }
                String[] parts = line.trim().split("\t");
                if (parts.length == 3) {
                    String movieId = parts[0];
                    double rating = Double.parseDouble(parts[1]);
                    int votes = Integer.parseInt(parts[2]);

                    if (votes >= minVotes) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("movieId", movieId);
                        params.put("rating", rating);
                        params.put("votes", votes);
                        batch.add(params);

                        if (batch.size() >= batchSize) {
                            updateBatch(batch, linesRead);
                            batch.clear();
                        }
                    }
                }
            }

            // Update any remaining movies in the final batch
            if (!batch.isEmpty()) {
                updateBatch(batch, linesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateBatch(List<Map<String, Object>> batch, int linesRead) {
        System.out.println("Updating batch of " + batch.size() + " movies. Total movies processed: " + linesRead);
        var query =
                "UNWIND $batch AS movie " +
                        "MATCH (m:Movie {id: movie.movieId}) " +
                        "SET m.rating = movie.rating, " +
                        "    m.votes = movie.votes";
        repository.batchUpdate(query, batch);
    }

    public void addOmdb(int topN, long milliPause) {
        var query = """
                MATCH (m:Movie {type: 'movie'})
                WHERE m.votes > 1000 AND m.plot IS NULL
                RETURN m.id, m.primaryTitle, m.votes
                ORDER BY m.votes DESC
                LIMIT $topN
                """;
        Map<String, Object> params = Map.of("topN", topN);
        var results = repository.queryForMap(query, params);
        var omdbClient = new OmdbClient();
        var index = 0;
        for (var result : results) {
            String imdb = (String) result.get("m.id");
            String title = (String) result.get("m.primaryTitle");
            long votes = (long) result.get("m.votes");
            try {
                var movieResponse = omdbClient.getMovieById(imdb);
                System.out.printf("%d: %s%n", ++index, movieResponse);
                addOmdbData(imdb, movieResponse);
                Thread.sleep(milliPause);
            } catch (Exception e) {
                System.err.println("Error fetching movie details for " + title + " with IMDb ID " + imdb + ": " + e);
            }
        }
    }

    private void addOmdbData(String imdb, MovieResponse movieResponse) {
        var query = new StringBuilder("""
                MATCH (m:Movie {id: $imdb})
                SET m.plot = $plot,
                        m.director = $director,
                        m.writer = $writer,
                        m.actors = $actors,
                        m.language = $language,
                        m.country = $country,
                        m.awards = $awards,
                        m.poster = $poster"""
        );

        Map<String, Object> params = new HashMap<>();
        params.put("imdb", imdb);
        params.put("plot", movieResponse.getPlot());
        params.put("director", movieResponse.getDirector());
        params.put("writer", movieResponse.getWriter());
        params.put("actors", movieResponse.getActors());
        params.put("language", movieResponse.getLanguage());
        params.put("country", movieResponse.getCountry());
        params.put("awards", movieResponse.getAwards());
        params.put("poster", movieResponse.getPoster());

        // Only add optional parameters if they exist
        if (movieResponse.getWebsite() != null) {
            query.append(", m.website = $website");
            params.put("website", movieResponse.getWebsite());
        }
        if (movieResponse.getBoxOffice() != null) {
            query.append(", m.boxoffice = $boxoffice");
            params.put("boxoffice", movieResponse.getBoxOffice());
        }

        System.out.println("Params: " + params);
        repository.update(query.toString(), params);
    }
}
