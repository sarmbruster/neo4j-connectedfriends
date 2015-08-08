package org.neo4j.connectedfriends;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.neo4j.connectedfriends.Labels.*;
import static org.neo4j.connectedfriends.RelationshipTypes.*;

/**
 * @author Stefan Armbruster
 */
public class FriendPathFinder {

    //    @Context
    public GraphDatabaseService graphDatabaseService;

    protected List<Node> nodes;

    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    public void createSampleGraph(int numberOfPeople, int avgFriends, double stdDev) {
        Random rnd = new Random();

        System.out.println("starting populating graph");
        nodes = new ArrayList<>(numberOfPeople);

        try (Transaction tx = graphDatabaseService.beginTx()) {
            for (int i = 0; i < numberOfPeople; i++) {
                Node node = graphDatabaseService.createNode(Person);
//                node.setProperty("id", i);
                nodes.add(node);
            }
            tx.success();
        }

        Transaction tx = graphDatabaseService.beginTx();
        int txCount = 0;

        for (int i = 0; i < numberOfPeople; i++) {
            long numberOfFriends = Math.round(avgFriends + rnd.nextGaussian() * stdDev);

            Node me = nodes.get(i);
            for (int f = 0; f < numberOfFriends; f++) {
                Node friend = nodes.get(rnd.nextInt(numberOfPeople));
                me.createRelationshipTo(friend, FRIEND_OF);
                txCount++;
                if (txCount % 10000 == 0) {
                    tx.success();
                    tx.close();
                    tx = graphDatabaseService.beginTx();
                }
            }
        }
        tx.success();
        tx.close();
        System.out.println("done populating graph");
    }

    public Path findPathWithMaxDepthViaSimplePaths(Node person1, Node person2, int maxDepth) {
        Path path = GraphAlgoFactory.allSimplePaths(PathExpanders.forType(FRIEND_OF), maxDepth).findSinglePath(
                person1, person2
        );
        return path;
    }

    public Path findPathWithMaxDepthViaShortestPath(Node person1, Node person2, int maxDepth) {
        Path path = GraphAlgoFactory.shortestPath(PathExpanders.forType(FRIEND_OF), maxDepth).findSinglePath(
                person1, person2
        );
        return path;
    }
}
