import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.MemoryIDMigrator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by halil on 24.09.2016.
 */
public class RecommendationServer {
    private final static MemoryIDMigrator memoryIDMigrator = new MemoryIDMigrator();
    private static UserBasedRecommender recommender;
    public static void main(String[] args) throws Exception {

        final double thresholdValue = 0.1;

        DataModel model = new FileDataModel(new File("connections.csv")) {
            @Override
            protected long readUserIDFromString(String stringID) {
                long result = memoryIDMigrator.toLongID(stringID);
                memoryIDMigrator.storeMapping(result, stringID);
                return result;
            }

            @Override
            protected long readItemIDFromString(String stringID){
                long result = memoryIDMigrator.toLongID(stringID);
                memoryIDMigrator.storeMapping(result, stringID);
                return result;
            }
        };
        UserSimilarity similarity = new TanimotoCoefficientSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(thresholdValue, similarity, model);
        recommender = new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity);

        startServer();
    }

    private static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/people", new RecommendHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }



    private static class RecommendHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = "This is the response";
            Map<String, String> params = queryToMap(httpExchange.getRequestURI().getQuery());
            String userID = params.get("userID");
            int per = Integer.parseInt(params.get("per"));
            if (per < 1) per = 24;
            int page = Integer.parseInt(params.get("page"));
            if (page < 1) page = 1;

            if (userID != null) {
                List<RecommendedItem> recommendations;
                try {
                    recommendations = recommender.recommend(memoryIDMigrator.toLongID(userID), page * per);
                    List<String> actorNames = getActorNamesFromIDs(recommendations.subList((page - 1) * per, page * per));
                    for (String actorName : actorNames)
                    {
                        System.out.println(actorName);
                        response += "\n" + actorName;
                    }
                } catch (NoSuchUserException e) {
                    //TODO: get regular actor names if the user doesn't follow anyone
                } catch (TasteException e) {
                    e.printStackTrace();
                }
            }
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private Map<String, String> queryToMap(String query){
            Map<String, String> result = new HashMap<String, String>();
            for (String param : query.split("&")) {
                String pair[] = param.split("=");
                if (pair.length>1) {
                    result.put(pair[0], pair[1]);
                }else{
                    result.put(pair[0], "");
                }
            }
            return result;
        }

        private List<String> getActorNamesFromIDs(List<RecommendedItem> recommendations)
        {
            String queryString = "select * from people where star_id in (";
            for (int i = 0; i < recommendations.size(); i++)
            {
                RecommendedItem item = recommendations.get(i);
                String starID = memoryIDMigrator.toStringID(item.getItemID());
                queryString += "\'" + starID + "\'";
                if (i == recommendations.size() - 1)
                    queryString += ")";
                else
                    queryString += ",";
            }

            Connection c;
            Statement stmt;
            List<String> actorNames = new ArrayList<String>();
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:movie_star_data.sqlite");
                c.setAutoCommit(false);

                stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery(queryString);
                while ( rs.next() ) {
                    String actorName = rs.getString("star_name");
                    actorNames.add(actorName);
                }
                rs.close();
                stmt.close();
                c.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return actorNames;
        }
    }
}
