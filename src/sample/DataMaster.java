package sample;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * DataMaster
 * Controls data handling to and from Web Data Source
 */
public class DataMaster {

    private String uri;
    private String method;

    /**
     * Constructor
     * @param uri uri to web service API
     * @param method HTTP Method (GET,POST,PUT,DELETE,HELP)
     */
    public DataMaster(String uri, String method) {
        this.uri = uri;
        this.method = method;
    }

    /**
     * Call API server and get data
     * @return List of Purchases from JSON provided by API
     * @throws IOException
     */
    protected List<Purchase> callAPI() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(this.uri).openConnection();

        conn.setRequestMethod(this.method);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");

        conn.setDoInput(true);
        conn.setDoOutput(false);

        String response = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
        List<Purchase> purchaseList = new Gson().fromJson(response, new TypeToken<List<Purchase>>() {}.getType());

        conn.disconnect();
        return purchaseList;
    }
}