
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */
    private Map<Long, Node> vertices;
    private Map<Long, HashSet<Long>> adj;


    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            vertices = new HashMap<>();
            adj = new HashMap<>();
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    static class Node implements Comparable<Node> {
        Long id;
        Double lon;
        Double lat;
        String name;

        /** The variable f is the priority of a node.
         * The variable g is the shortest known path distance from s to the node.
         * The variable h is the heuristic distance, the distance from the node to
         * the target location.
         * f is used in A* search to find the shortest route from the start location
         * to end location.
         */
        double f;
        double g;
        double h;
        boolean marked;

        Node(Long id, Double lon, Double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            marked = false;
        }
        public void setName(String n) {
            this.name = n;
        }

        public boolean equals(Node otherNode) {
            if (otherNode == null) {
                return false;
            }
            if (this == otherNode) {
                return true;
            }


            if (this.id == otherNode.id || this.lon == otherNode.lon || this.lat == otherNode.lat) {
                return true;
            }
            return false;
        }

        public int compareTo(Node otherNode) {
            if (this.f < otherNode.f) {
                return -1;
            } else if (this.f > otherNode.f) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    static class Way {
        ArrayList<Long> nodeIds;
        Long wayID;
        String name;
        String maxSpeed;

        Way(Long id) {
            this.wayID = id;
            nodeIds = new ArrayList<>();
        }

        public void addWay(Long id) {
            nodeIds.add(id);
        }

        public void setName(String n) {
            this.name = n;
        }

        public void setSpeed(String s) {
            this.maxSpeed = s;
        }
    }

    public Map<Long, Node> getVertices() {
        return vertices;
    }


    public void addNode(Long id, Double lon, Double lat) {
        vertices.put(id, new Node(id, lon, lat));
    }

    public void addEdge(Node n1, Node n2) {
        if (adj.containsKey(n1.id)) {
            HashSet<Long> adjacent = adj.get(n1.id);
            adjacent.add(n2.id);
        } else {
            adj.put(n1.id, new HashSet<>());
            HashSet<Long> adjacent = adj.get(n1.id);
            adjacent.add(n2.id);
        }
    }

    public void removeNode(Long id) {
        vertices.remove(id);
        adj.remove(id);
    }

    public Node returnCopy(Long id) {
        return new Node(id, lon(id), lat(id));
    }

    public void setName(Long id, String n) {
        Node thisNode = vertices.get(id);
        thisNode.name = n;
    }

    public void addWay(Way way) {
        for (int i = 0; i < way.nodeIds.size() - 1; i++) {
            Long curr = way.nodeIds.get(i);
            Long next = way.nodeIds.get(i + 1);

            addEdge(vertices.get(curr), vertices.get(next));
            addEdge(vertices.get(next), vertices.get(curr));
        }
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        List<Long> toDelete = new ArrayList<>();
        for (Long vertexID: vertices.keySet()) {
            if (!adj.containsKey(vertexID)) {
                toDelete.add(vertexID);
            }
        }
        for (Long vertex: toDelete) {
            removeNode(vertex);
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        return vertices.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return adj.get(v);

    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double minDistance = 100000;
        double currentDistance;
        long vertexID = 0;
        for (Node vertex: vertices.values()) {
            currentDistance = distance(lon, lat, vertex.lon, vertex.lat);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                vertexID = vertex.id;
            }
        }
        return vertexID;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return vertices.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return vertices.get(v).lat;
    }
}
