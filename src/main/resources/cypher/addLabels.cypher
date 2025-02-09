CREATE INDEX person_name_index FOR (p:Person) ON (p.name);

//CREATE INDEX movie_name_index FOR (m:Movie) ON (m.primaryTitle);

// Add general label so we can remove Movie
CALL apoc.periodic.iterate(
'MATCH (m:Movie) RETURN m',
'SET m:Film',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (m:Film) WHERE m.type = 'videoGame' RETURN m",
'DETACH DELETE m',
{batchSize: 1000}
);


CALL apoc.periodic.iterate(
"MATCH (m:Film) WHERE m.type CONTAINS 'tv' RETURN m",
'REMOVE m:Movie SET m:TV',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (m:Film) WHERE 'Documentary' in m.genres RETURN m",
'REMOVE m:Movie SET m:Documentary',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (m:Movie) WHERE m.type = 'short' RETURN m",
'REMOVE m:Movie SET m:Short',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'actor' IN p.primaryProfession RETURN p",
"SET p:Actor, p.gender = 'M'",
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'actress' IN p.primaryProfession RETURN p",
"SET p:Actress, p.gender = 'F'",
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'director' IN p.primaryProfession RETURN p",
'SET p:Director',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'writer' IN p.primaryProfession RETURN p",
'SET p:Writer',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'composer' IN p.primaryProfession RETURN p",
'SET p:Composer',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'cinematographer' IN p.primaryProfession RETURN p",
'SET p:Cinematographer',
{batchSize: 1000}
);


CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'production_designer' IN p.primaryProfession RETURN p",
'SET p:ProductionDesigner',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'costume_designer' IN p.primaryProfession RETURN p",
'SET p:CostumeDesigner',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'visual_effects' IN p.primaryProfession RETURN p",
'SET p:VisualEffects',
{batchSize: 1000}
);

CALL apoc.periodic.iterate(
"MATCH (p:Person) WHERE 'art_director' IN p.primaryProfession RETURN p",
'SET p:ArtDirector',
{batchSize: 1000}
);
