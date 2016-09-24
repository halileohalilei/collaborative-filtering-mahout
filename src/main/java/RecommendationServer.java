import com.sun.net.httpserver.HttpServer;
import org.apache.mahout.cf.taste.impl.model.MemoryIDMigrator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

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
        server.createContext("/people", new RecommendationHandler(recommender, memoryIDMigrator));
        server.createContext("/follow", new FollowHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }




}
