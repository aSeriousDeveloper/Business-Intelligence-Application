package sample;

public class Purchase {

    private Integer QTR;
    private Integer Quantity;
    private String Region;
    private String Vehicle;
    private Integer Year;

    public Integer getQTR() {
        return this.QTR;
    }

    public void setQTR(Integer QTR) {
        this.QTR = QTR;
    }

    public Integer getYear() {
        return this.Year;
    }

    public void setYear(Integer year) {
        this.Year = year;
    }

    public String getRegion() {
        return this.Region;
    }

    public void setRegion(String region) {
        this.Region = region;
    }

    public String getVehicle() {
        return this.Vehicle;
    }

    public void setVehicle(String vehicle) {
        this.Vehicle = vehicle;
    }

    public Integer getQuantity() {
        return this.Quantity;
    }

    public void setQuantity(Integer quantity) {
        this.Quantity = quantity;
    }

}
