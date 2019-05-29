package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    //Thread for refreshing data
    private Thread refreshThread;

    //Do Not Filter
    private List<Purchase> purchaseListComplete;
    //Filter this one
    private List<Purchase> purchaseListFiltered;

    //Stores filters for data
    private Purchase purchaseFilters = new Purchase();

    //List unique values to display in filter selectors
    private ObservableList<String> regionList = FXCollections.observableArrayList();
    private ObservableList<String> vehicleList = FXCollections.observableArrayList();
    private ObservableList<String> yearSelection = FXCollections.observableArrayList();
    private ObservableList<String> quarterSelection = FXCollections.observableArrayList();

    //List operators for quantity values, and variable for which is selected
    private ObservableList<String> quantitySelection = FXCollections.observableArrayList("All", "=", "!=", ">", ">=", "<", "<=");
    private String selectedOperator;

    @FXML
    private BorderPane applicationPanel;

    @FXML
    private GridPane loadingPanel;

    @FXML
    private CheckMenuItem autoRefresh;

    @FXML
    private MenuItem lastUpdated;

    @FXML
    private Tab barChartTab;

    @FXML
    private Tab pieChartTab;

    @FXML
    private Tab lineGraphTab;

    @FXML
    private ChoiceBox quarterChoice;

    @FXML
    private ChoiceBox quantityChoice;

    @FXML
    private TextField quantityFill;

    @FXML
    private ChoiceBox regionChoice;

    @FXML
    private ChoiceBox vehicleChoice;

    @FXML
    private ChoiceBox yearChoice;

    /**
     * On Window Open:
     * display loading panel,
     * activate auto refresh,
     * set manual refresh to manual refresh button,
     * then get data from online server
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        showLoadingPanel();
        autoRefresh.setSelected(true);
        getData();
    }

    /**
     * Display Loading Panel
     * Build loading panel with app title, loading bar and loading text
     * This will be used during the loading process and then disabled
     */
    private void showLoadingPanel() {
        //Loading title
        Label loadTitle = new Label("Business Intelligence");
        loadTitle.setStyle("-fx-font-size: 16px");
        loadTitle.setTextAlignment(TextAlignment.CENTER);

        //Loading label to say data is loading
        Label loadText = new Label("Loading Data...");
        loadText.setStyle("-fx-font-size: 12px");
        loadText.setTextAlignment(TextAlignment.CENTER);

        //Progress bar at bottom of screen
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefSize(768, 16);

        //Place elements into grid
        loadingPanel.add(loadTitle, 1, 0);
        loadingPanel.add(loadText, 1, 1);
        loadingPanel.add(progressBar, 1, 2);
    }

    /**
     * Get data from main server &
     * display last time refreshed, data table, barchart and pie chart
     * Setup thread for auto-updating and updating the data in the background via thread
     */
    private void getData() {
        //Enable loading panel, disable main panel
        loadingPanel.setVisible(true);
        applicationPanel.setVisible(false);

        //Build new background thread for data refresh
        refreshThread = new Thread(() -> {
            //On first time...
            boolean initialising = true;
            while (true) {
                    try {
                        //if auto-refreshing or on first time...
                        if(autoRefresh.isSelected() || initialising) {
                            //get data from server and put into List
                            DataMaster dataMaster = new DataMaster(
                                    "http://glynserver2.cms.livjm.ac.uk/DashService/SGetSales",
                                    "GET");
                            purchaseListComplete = dataMaster.callAPI();

                            //On first time, make sure filtered list matches full list
                            if (initialising) {
                                purchaseListFiltered = purchaseListComplete;
                                initialising = false;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        //If no error...
                        if (purchaseListFiltered != null) {
                            Platform.runLater(() -> {
                                //Disable loading panel, enable main panel
                                loadingPanel.setVisible(false);
                                applicationPanel.setVisible(true);

                                //Update last refresh
                                DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MMM d HH:mm:ss");
                                LocalDateTime now = LocalDateTime.now();
                                lastUpdated.setText("Last Refresh: " + dateTime.format(now));

                                //Build data and filter options
                                buildData();
                                buildFilters();

                            });
                        }
                    }

                try {
                    //Sleep for 5 minutes
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        });

        //Run thread, do not allow to keep running on app close
        refreshThread.setDaemon(true);
        refreshThread.start();

    }

    /**
     * Build viewable data for the user:
     * data table,
     * bar graph,
     * pie chart,
     * line graph
     */
    private void buildData() {
        applicationPanel.setRight(buildDataTable());

        barChartTab.setContent(buildBarChart(
                "Vehicle Purchase Volume Per Annum",
                "Year/Vehicle Type",
                "Purchase Volume"));

        pieChartTab.setContent(buildPieChart(
                "Total Vehicle Sales by Type"));

        lineGraphTab.setContent(buildLineGraph(
                "Yearly Sales by Vehicle",
                "Year",
                "Purchase Volume"));
    }

    /**
     * Build table of all filtered data
     * Column for each type of data
     * @return TableView table to display
     */
    private TableView buildDataTable() {
        //build new table
        TableView dataTable = new TableView();
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        //QTR columnn
        TableColumn<String, Purchase> quarterColumn = new TableColumn<>("QTR");
        quarterColumn.setCellValueFactory(new PropertyValueFactory<>("QTR"));

        //quantity columnn
        TableColumn<String, Purchase> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("Quantity"));

        //region columnn
        TableColumn<String, Purchase> regionColumn = new TableColumn<>("Region");
        regionColumn.setCellValueFactory(new PropertyValueFactory<>("Region"));

        //vehicle columnn
        TableColumn<String, Purchase> vehicleColumn = new TableColumn<>("Vehicle");
        vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("Vehicle"));

        //year columnn
        TableColumn<String, Purchase> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("Year"));

        //aggregate columns
        dataTable.getColumns().addAll(quarterColumn, quantityColumn, regionColumn, vehicleColumn, yearColumn);

        //add data to columns
        for (int i = 0; i < purchaseListFiltered.size() - 1; i++) {
            dataTable.getItems().add(purchaseListFiltered.get(i));
        }

        //return completed data table
        return dataTable;
    }

    /**
     * Builds the bar chart to display to user
     * Years are separate bars, with vehicle type on X axis and purchase volume on Y axis
     * @param title title of graph
     * @param xAxis X axis title
     * @param yAxis Y axis title
     * @return Bar Chart to display
     */
    private Node buildBarChart(String title, String xAxis, String yAxis) {
        CategoryAxis x = new CategoryAxis();
        x.setLabel(xAxis);

        NumberAxis y = new NumberAxis();
        y.setLabel(yAxis);

        BarChart barChart = new BarChart(x, y);
        barChart.setTitle(title);

        barChart.getData().clear();

        //Create Hash Map and XY Series for adding data to bar chart 
        List<XYChart.Series> barSeries = new ArrayList<>();
        HashMap<Integer, HashMap<String, Integer>> barData = new HashMap<>();

        //Add all values from filtered Purchases List into HashMap
        for (Purchase purchase : purchaseListFiltered) {
            //If year does not yet exist in data series...
            if (!barData.containsKey(purchase.getYear())) {
                barData.put(purchase.getYear(), new HashMap<>());

                XYChart.Series series = new XYChart.Series();
                series.setName(purchase.getYear().toString());

                barSeries.add(series);
            }
            
            //HashMap for innter data (vehicle type & purchase volume)
            HashMap<String, Integer> innerData = barData.get(purchase.getYear());
            //if vehicle type already exists...
            if(innerData.containsKey(purchase.getVehicle())) {
                //add purchase volume to current tally for the year
                innerData.replace(purchase.getVehicle(), purchase.getQuantity() + innerData.get(purchase.getVehicle()));
            } else {
                //otherwise create a new hash for next vehicle
                innerData.put(purchase.getVehicle(), purchase.getQuantity());
            }
        }

        //iterate through data and add to bar chart directly
        int i = 0;
        for(Integer year : barData.keySet()) {
            HashMap<String, Integer> inner = barData.get(year);
            for (String key : inner.keySet()) {
                //For each year, add vehicle and purchase volume to the chart
                barSeries.get(i).getData().add(new XYChart.Data<>(key, inner.get(key))); {
                }
            }
            barChart.getData().add(barSeries.get(i));
            i++;
        }
        return barChart;
    }

    /**
     * Builds pie chart to display to user
     * Slices are split by vehicle, size of slice is overall quantity for all dates
     * @param title preferred title of pie chart
     * @return Node, complete pie chart data
     */
    private Node buildPieChart(String title) {
        HashMap<String, Integer> pieChart = new HashMap<>();

        //go through filtered purchase list
        for (Purchase purchase : purchaseListFiltered) {
            //if vehicle type is unique, add as new vehicle type
            if(!pieChart.containsKey(purchase.getVehicle())) {
                pieChart.put(purchase.getVehicle(), purchase.getQuantity());
            } else {
                //otherwise, add to current vehicle the additional quanitity amount
                pieChart.put(purchase.getVehicle(), (pieChart.get(purchase.getVehicle()) + purchase.getQuantity()));
            }
        }
        
        //Create list for inputting into pie chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        //for every entry in the Hash Map...
        for(Map.Entry mapPurchase : pieChart.entrySet()) {
            //add piece of data to the pie chart
            String key = (String) mapPurchase.getKey();
            pieData.add(new PieChart.Data(key, (Integer) mapPurchase.getValue()));
        }

        //create pie chart and send
        PieChart pie = new PieChart(pieData);
        pie.setTitle(title);
        return pie;

    }

    /**
     * Build line graph from filtered data
     * Each line is a vehicle, y axis is volume over purchases
     * x axis is time over years
     * TODO possibly change this so it includes quarterly values too
     * @param title title for graph
     * @param xAxis x axis title
     * @param yAxis y axis title
     * @return Node complete line graph
     */
    private Node buildLineGraph(String title, String xAxis, String yAxis) {
        CategoryAxis x = new CategoryAxis();
        x.setLabel(xAxis);

        NumberAxis y = new NumberAxis();
        y.setLabel(yAxis);

        LineChart lineGraph = new LineChart(x, y);
        lineGraph.setTitle(title);

        //clear current graph data
        lineGraph.getData().clear();

        //Build XY Series List and Hashmap for data
        List<XYChart.Series> lineSeries = new ArrayList<>();
        HashMap<String, HashMap<String, Integer>> lineData = new HashMap<>();

        //for every filtered purchase...
        for (Purchase purchase : purchaseListFiltered) {
            //if line chart data does not currently contain current vehicle...
            if (!lineData.containsKey(purchase.getVehicle())) {
                //add to hashmap as new hash
                lineData.put(purchase.getVehicle(), new HashMap<>());

                XYChart.Series series = new XYChart.Series();
                series.setName(purchase.getVehicle());
                //add to XY Series as new Series type
                lineSeries.add(series);
            }

            //Get hashmap for current vehicle
            HashMap<String, Integer> innerData = lineData.get(purchase.getVehicle());
            //if vehicle already exists in hashmap...
            if(innerData.containsKey(purchase.getYear().toString())) {
                //Add to current year value the next purchase volume
                innerData.replace(purchase.getYear().toString(), purchase.getQuantity() + innerData.get(purchase.getYear().toString()));
            }
            else {
                //otherwise, add current year to data hash along with its quantity
                innerData.put(purchase.getYear().toString(), purchase.getQuantity());
            }
        }

        //iterate through all of the data
        int i = 0;
        for(String vehicle: lineData.keySet()) {
            HashMap<String, Integer> inner = lineData.get(vehicle);
            for (String key : inner.keySet()) {
                //add each item to the XY series
                lineSeries.get(i).getData().add(new XYChart.Data<>(key, inner.get(key))); {
                }
            }
            lineGraph.getData().add(lineSeries.get(i));
            i++;
        }
        return lineGraph;

    }

    /**
     * Add options to filter boxes from data set
     * Add listeners to filter boxes on usage
     */
    private void buildFilters() {
        resetFilterValues();

        //build filter list for quarters
        quarterSelection = buildFilterList(quarterSelection, purchaseListComplete, "QTR");
        quarterChoice.setItems(quarterSelection);
        //add listener so that when quarter filter is changed, the data is filtered
        quarterChoice.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                if(newValue.equals("All")) {
                    newValue = "0";
                }
                this.purchaseFilters.setQTR(Integer.parseInt(newValue.toString()));
                filterPurchases();
            }
        });

        //quantitiy choice box uses preset values for filter, assign those and build filter listener
        quantityChoice.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            this.selectedOperator = newValue.toString();
            if(this.purchaseFilters.getQuantity() != null) {
                filterPurchases();
            }
        });

        //assign listener to quantity fill box
        this.quantityFill.textProperty().addListener((observable, oldValue, newValue) -> {
            //if value isn't an integer, set to old value
            if(!newValue.matches("\\d{0,10}?")) {
                quantityFill.setText(oldValue);
            }
            //if 0 length, set to null
            else if (newValue.length() == 0) {
                purchaseFilters.setQuantity(null);
            }
            //otherwise value is fine, filter
            else {
                purchaseFilters.setQuantity(Integer.parseInt(newValue));
            }
        });

        //Build region filter list and assign listener to filter
        regionList = buildFilterList(regionList, purchaseListComplete, "Region");
        regionChoice.setItems(regionList);
        regionChoice.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                this.purchaseFilters.setRegion(newValue.toString());
                filterPurchases();
            }
        });

        //Build vehicle filter list and assign listener to filter
        vehicleList = buildFilterList(vehicleList, purchaseListComplete, "Vehicle");
        vehicleChoice.setItems(vehicleList);
        vehicleChoice.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                this.purchaseFilters.setVehicle(newValue.toString());
                filterPurchases();
            }
        });

        //Build year filter list and assign listener to filter
        yearSelection = buildFilterList(yearSelection, purchaseListComplete, "Year");
        yearChoice.setItems(yearSelection);
        yearChoice.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                if(newValue.equals("All")) {
                    newValue = "0";
                }
                this.purchaseFilters.setYear(Integer.parseInt(newValue.toString()));
                filterPurchases();
            }
        });
    }

    /**
     * Build a list of values for a given filter
     * @param filterList list to include filters
     * @param completeList complete set of data
     * @param type Type of data to add to filter list (QTR, Quantity, Region, Vehicle, Year)
     * @return ObservableList of filters for given data type
     */
    private ObservableList<String> buildFilterList(ObservableList<String> filterList, List<Purchase> completeList, String type) {
        try {
            //for every purchase in the unfiltered list...
            for (Purchase purchase : completeList) {
                //build get method using given type
                Method method = purchase.getClass().getMethod("get" + type);
                if(!filterList.contains(method.invoke(purchase).toString())) {
                    //if filter list doesn't currently contain given value, add to filter list
                    filterList.add(method.invoke(purchase).toString());
                }
            }
            //return filter list for given type
            return filterList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filterList;
    }

    /**
     * Apply filters from purchaseFilters object
     * The builds data using filtered purchase data
     */
    private void filterPurchases() {
        //make sure you're filtering from full data first...
        purchaseListFiltered = purchaseListComplete;

        //if QTR has applied filter...
        //use stream.filter to get only values that match the filter value
        if(this.purchaseFilters.getQTR() != null && purchaseFilters.getQTR() != 0) {
            purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                    purchase.getQTR().equals(purchaseFilters.getQTR())).collect(Collectors.toCollection(ArrayList::new));
        }

        //if Quantity has applied filter and there's a selected oprator...
        if(this.purchaseFilters.getQuantity() != null && selectedOperator != null) {
            //perform mathematical comparison on data for filter, must match operand ans given value
            switch (selectedOperator) {
                case "=": {
                    this.purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                            purchase.getQuantity() == purchaseFilters.getQuantity()).collect(Collectors.toCollection(ArrayList::new));
                    break;
                }
                case "!=": {
                    this.purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                            purchase.getQuantity() != purchaseFilters.getQuantity()).collect(Collectors.toCollection(ArrayList::new));
                    break;
                }
                case ">": {
                    this.purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                            purchase.getQuantity() > purchaseFilters.getQuantity()).collect(Collectors.toCollection(ArrayList::new));
                    break;
                }
                case ">=": {
                    this.purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                            purchase.getQuantity() >= purchaseFilters.getQuantity()).collect(Collectors.toCollection(ArrayList::new));
                    break;
                }
                case "<": {
                    this.purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                            purchase.getQuantity() < purchaseFilters.getQuantity()).collect(Collectors.toCollection(ArrayList::new));
                    break;
                }
                case "<=": {
                    this.purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                            purchase.getQuantity() <= purchaseFilters.getQuantity()).collect(Collectors.toCollection(ArrayList::new));
                    break;
                }
            }
        }

        //if region has applied filter...
        //use stream.filter to get only values that match the filter value
        if(this.purchaseFilters.getRegion() != null && !this.purchaseFilters.getRegion().equals("All")) {
            purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                    purchase.getRegion().equals(purchaseFilters.getRegion())).collect(Collectors.toCollection(ArrayList::new));
        }

        //if vehicle has applied filter...
        //use stream.filter to get only values that match the filter value
        if(this.purchaseFilters.getVehicle() != null && !this.purchaseFilters.getVehicle().equals("All")) {
            purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                    purchase.getVehicle().equals(purchaseFilters.getVehicle())).collect(Collectors.toCollection(ArrayList::new));
        }

        //if year has applied filter...
        //use stream.filter to get only values that match the filter value
        if(this.purchaseFilters.getYear() != null && purchaseFilters.getYear() != 0) {
            purchaseListFiltered = purchaseListFiltered.stream().filter(purchase ->
                    purchase.getYear().equals(purchaseFilters.getYear())).collect(Collectors.toCollection(ArrayList::new));
        }

        //rebuild data
        buildData();
    }

    /**
     * clear values from filter boxes
     * Add index 0 option for "all"
     */
    private void resetFilterValues() {
        quarterSelection.clear();
        quarterSelection.add(0, "All");

        quantityChoice.setItems(quantitySelection);
        selectedOperator = "All";

        regionList.clear();
        regionList.add(0, "All");

        vehicleList.clear();
        vehicleList.add(0, "All");

        yearSelection.clear();
        yearSelection.add(0, "All");

        resetChoiceBoxes();
    }

    /**
     * Reset all filter boxes to all option
     * clear value from quantity fill box
     */
    private void resetChoiceBoxes() {
        quarterChoice.setValue("All");

        quantityFill.setText("");
        quantityChoice.setValue("All");
        purchaseFilters.setQuantity(null);

        vehicleChoice.setValue("All");
        regionChoice.setValue("All");
        yearChoice.setValue("All");
    }

    @FXML
    private void manualRefreshAction() {
        getData();
    }

    /**
     * Open about screen to display about information
     */
    @FXML
    private void aboutMenu() {
        try {
            //Load about FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("About.fxml"));
            Parent rootAbout = fxmlLoader.load();

            //Prepare, create and load stage
            //Stage is always on top and must be closed before continuing
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("About");
            stage.setAlwaysOnTop(true);
            stage.setResizable(false);
            stage.setScene(new Scene(rootAbout));
            stage.show();

        } catch (Exception e) {e.printStackTrace();}
    }

    /**
     * Pressing "exit button" exits application
     * uses System.exit(0)
     */
    @FXML
    private void exitAction() {
        System.exit(0);
    }

}
