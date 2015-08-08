package org.neo4j.connectedfriends

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils

import static spock.util.matcher.HamcrestMatchers.closeTo

import org.neo4j.graphdb.Node

import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class FriendPathFinderSpec extends Specification {

    @Rule
    @Delegate
    Neo4jResource neo4j = new Neo4jResource()

    @Rule
    TemporaryFolder tmp;

    def numberOfPeople = 1000
    def avgFriends = 25
    def stdDev  = 10
    def maxDepth = 4
    FriendPathFinder cut

    def setup() {
        cut = new FriendPathFinder(graphDatabaseService: graphDatabaseService)
        cut.createSampleGraph(numberOfPeople, avgFriends, stdDev)
    }

    def "check sample graph"() {
        when:

        int numberOfNodes = 0
        double summedDegree = 0
        Neo4jUtils.withSuccessTransaction(graphDatabaseService) {
            for (Node n in graphDatabaseService.findNodes(Labels.Person)) {
                numberOfNodes++
                summedDegree += n.getDegree()
            }
        }

        then:
        numberOfNodes == numberOfPeople
        summedDegree closeTo( avgFriends * 2 * numberOfNodes, 5*numberOfNodes)

    }

//    @Benchmark
//    @Warmup(iterations = 10)
//    @Measurement(iterations = 5)
//    @Fork(3)
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    def "check if two people are connected"() {
        when:
        def rnd = new Random()
        def path = cut.findPathWithMaxDepthViaSimplePaths(
                rnd.nextInt(numberOfPeople), rnd.nextInt(numberOfPeople), maxDepth
        )

        then:
        path != null
        path.length() <= 4
    }

}
