package org.neo4j.connectedfriends;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Stefan Armbruster
 */
@State(Scope.Benchmark)
//@Warmup(iterations = 2)
//@Measurement(iterations = 5)
//@Fork(value = 2)
@BenchmarkMode(Mode.AverageTime) // Mode.Throughput
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class Benchmarks {

    public static final int numberOfPeople = 1000;
    public static final int avgFriends = 25;
    public static final int stdDev = 10;
    public static final int maxDepth = 4;

//    private Node person1;
//    private Node person2;

    private GraphDatabaseService graphDatabaseService;
    private FriendPathFinder friendPathFinder;
    private File folder;
    private Random random = new Random();

    @Setup
    public void setup() throws IOException {
//        System.out.println("setup");
        folder = new File("/tmp/graph.db");
        boolean isFresh = folder.exists();
        graphDatabaseService = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(folder.getAbsolutePath())
//                .setConfig(GraphDatabaseSettings.cache_type, "none")
                .newGraphDatabase();
                //.newImpermanentDatabase();
        friendPathFinder = new FriendPathFinder();
        friendPathFinder.setGraphDatabaseService(graphDatabaseService);

        if (isFresh) {
            friendPathFinder.createSampleGraph(numberOfPeople, avgFriends, stdDev);
        }
    }

    @TearDown
    public void tearDown() {
        graphDatabaseService.shutdown();
//        folder.delete();
    }

    @Benchmark
    public void checkIfTwoPeopleAreConnectedTraversalApi(Blackhole blackhole) {
        try (Transaction tx=graphDatabaseService.beginTx()) {
            Node person1 = graphDatabaseService.getNodeById(random.nextInt(numberOfPeople));
            Node person2 = graphDatabaseService.getNodeById(random.nextInt(numberOfPeople));

            Path p = friendPathFinder.findPathWithMaxDepth(person1, person2, maxDepth);
            for (Node n : p.nodes()) {
                blackhole.consume(n);
            }
            tx.success();
        }
    }

    @Benchmark
    public void checkIfTwoPeopleAreConnectedCypher(Blackhole blackhole) {
        try (Transaction tx=graphDatabaseService.beginTx()) {
            Map<String,Object> params = MapUtil.map(
                    "p1", random.nextInt(numberOfPeople),
                    "p2", random.nextInt(numberOfPeople));
            Result result = graphDatabaseService.execute("match p=(p1)-[:FRIEND_OF*..4]-(p2) where id(p1)={p1} and id(p2)={p2} return length(p) as l limit 1", params);
            Map<String, Object> r = IteratorUtil.singleOrNull(result);
            blackhole.consume(r);
            tx.success();
        }
    }
}
