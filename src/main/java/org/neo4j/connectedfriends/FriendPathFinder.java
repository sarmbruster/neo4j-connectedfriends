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

        nodes = new ArrayList<>(numberOfPeople);
        for (int i = 0; i < numberOfPeople; i++) {
            try (Transaction tx = graphDatabaseService.beginTx()) {
                Node node = graphDatabaseService.createNode(Person);
                node.setProperty("id", i);
                nodes.add(node);
                tx.success();
            }
        }

        for (int i = 0; i < numberOfPeople; i++) {
            long numberOfFriends = Math.round(avgFriends + rnd.nextGaussian() * stdDev);

            Node me = nodes.get(i);
            try (Transaction tx = graphDatabaseService.beginTx()) {
                for (int f = 0; f < numberOfFriends; f++) {
                    Node friend = nodes.get(rnd.nextInt(numberOfPeople));
                    me.createRelationshipTo(friend, FRIEND_OF);
                }
                tx.success();
            }
        }
    }

    public Path findPathWithMaxDepth(Node person1, Node person2, int maxDepth) {
//            Node node1 = graphDatabaseService.findNode(Person, "id", person1Id);
//            Node node2 = graphDatabaseService.findNode(Person, "id", person2Id);
        Path path = GraphAlgoFactory.allSimplePaths(PathExpanders.forType(FRIEND_OF), maxDepth).findSinglePath(
                person1, person2
        );
        return path;
    }
}
