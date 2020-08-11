import java.util.HashMap;
import java.util.Map;


/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private int depth;
    private static final double TOTALLONWIDTH = MapServer.ROOT_LRLON - MapServer.ROOT_ULLON;
    private static final double TOTALLATHEIGHT = MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT;
    private int[] coordinates;
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private double rasterULLON;
    private double rasterULLAT;
    private double rasterLRLON;
    private double rasterLRLAT;

    public Rasterer() {
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        Map<String, Object> results = new HashMap<>();
        System.out.println(params);

        /* Extract coordinates and box size from user requested query */
        double queryLRLON = params.get("lrlon");
        double queryULLON = params.get("ullon");
        double queryLRLAT = params.get("lrlat");
        double queryULLAT = params.get("ullat");
        double width = params.get("w");

        /* Calculate coordinates and determine which image files are needed for the
        query that will be sent ot the front end.
         */
        depth = calculateDepth(calculateLonDPP(queryLRLON, queryULLON, width));
        coordinates = calculateStartXY(queryLRLON, queryULLON, queryLRLAT, queryULLAT);
        String[][] renderGrid = getFileNames();
        setRasterCoords();

        /* Place results into a Map<String, Object> and return to the client.
        This data will be interpreted and displayed as an image in the web browser.
         */
        results.put("render_grid", renderGrid);
        results.put("raster_ul_lon", rasterULLON);
        results.put("raster_ul_lat", rasterULLAT);
        results.put("raster_lr_lon", rasterLRLON);
        results.put("raster_lr_lat", rasterLRLAT);
        results.put("depth", depth);
        results.put("query_success", querySuccess(queryLRLON, queryULLON, queryLRLAT, queryULLAT));
        return results;
    }

    /**
     * Calculates the longitudinal distance per pixel of a query box or image.
     * @param lrLON
     * @param ulLON
     * @param width
     * @return
     */
    private double calculateLonDPP(double lrLON, double ulLON, double width) {
        double lonDDP = (lrLON - ulLON) / width;
        return lonDDP;
    }

    /**
     * Calculates the required depth (or resolution) needed to display the requested
     * query. Depth is calculated by finding the largest LonDPP that less than or equal
     * to the LonDDP of the requested query box. A higher depth
     * corresponds with higher resolution. Depth identifies which set of images we need to use.
     * @param queryLonDDP
     * @return
     */
    private static int calculateDepth(double queryLonDDP) {
        double rasterLonDDP;
        double diffInLon;
        int lonOfOneTile;
        for (int i = 0; i <= 7; i++) {
            lonOfOneTile = Math.max(1, (int) Math.pow(2, i));
            diffInLon = TOTALLONWIDTH / lonOfOneTile;
            rasterLonDDP = diffInLon / MapServer.TILE_SIZE;
            if (rasterLonDDP <= queryLonDDP) {
                return i;
            }
        }
        return 7;
    }

    /**
     * Images are stored and named in a grid format. This method calculates the range of
     * images that contain the requested query. startX and startY are the coordinates of the
     * upper-left image. endX and endY are the coordinates of the upper-right image.
     * are the coordinates
     * @param lrLON
     * @param ulLON
     * @param ulLAT
     * @param lrLAT
     * @return
     */
    private int[] calculateStartXY(double lrLON, double ulLON, double lrLAT, double ulLAT) {
        int numOfTiles = (int) Math.pow(2.0, depth);
        int lastTile = (int) Math.pow(2.0, depth) - 1;
        double tileWidth = TOTALLONWIDTH / numOfTiles;
        double tileHeight = TOTALLATHEIGHT / numOfTiles;
        startX = (int) Math.floor((ulLON - MapServer.ROOT_ULLON) / tileWidth);
        startY = (int) Math.floor((MapServer.ROOT_ULLAT - ulLAT) / tileHeight);
        endX = lastTile - (int) Math.floor((MapServer.ROOT_LRLON - lrLON) / tileWidth);
        endY = lastTile - (int) Math.floor((lrLAT - MapServer.ROOT_LRLAT) / tileHeight);
        coordinates = new int[]{startX, startY, endX, endY};
        return coordinates;
    }

    /**
     * Stores the filenames of images in a String[][]. Images are named as such: "d2_x3_y1.png",
     * where '2' is the level of depth, '3' is the x coordinate, and '1' is the y coordinate.
     * @return
     */
    private String[][] getFileNames() {
        int xRange = endX - startX + 1;
        int yRange = endY - startY + 1;
        String[][] renderGrid = new String[yRange][xRange];
        for (int i = 0; i < yRange; i++) {
            for (int j = 0; j < xRange; j++) {
                renderGrid[i][j] = "d" + depth + "_x" + (startX + j) + "_y" + (startY + i) + ".png";
            }
        }
        return renderGrid;
    }

    /**
     * This method sets the coordinates of the rastered image.
     */
    private void setRasterCoords() {
        int numOfTiles = (int) (Math.pow(2.0, depth));
        double tileWidth = TOTALLONWIDTH / numOfTiles;
        double tileHeight = TOTALLATHEIGHT / numOfTiles;
        rasterULLON = MapServer.ROOT_ULLON + (tileWidth * (startX));
        rasterULLAT = MapServer.ROOT_ULLAT - (tileHeight * startY);
        rasterLRLON = MapServer.ROOT_LRLON - (tileWidth * (numOfTiles - 1 - endX));
        rasterLRLAT = MapServer.ROOT_LRLAT + (tileHeight * (numOfTiles - 1 - endY));
    }

    /**
     * This method returns true if the query is successful, and false otherwise.
     * If the query box is completely outside of the root longitude/latitudes,
     * this method will return false. It also returns false if the query box
     * coordinates are nonsensical.
     * @param lrLON
     * @param ulLON
     * @param lrLAT
     * @param ulLAT
     * @return
     */
    private boolean querySuccess(double lrLON, double ulLON, double lrLAT, double ulLAT) {
        if (ulLAT < lrLAT || ulLON > lrLON) {
            return false;
        }
        return !(ulLON < MapServer.ROOT_ULLON || lrLON > MapServer.ROOT_LRLON
                || ulLAT > MapServer.ROOT_ULLAT || lrLAT < MapServer.ROOT_LRLON);
    }
}
