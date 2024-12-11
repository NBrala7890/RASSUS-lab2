package hr.fer.tel.rassus.models;

import com.google.gson.Gson;

import java.util.Objects;

public class SensorModel {

    private static final Gson gson = new Gson();

    private int id;

    private String address;

    private int port;

    public SensorModel(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public static String toJson(SensorModel sensorModel) {

        return gson.toJson(sensorModel);

    }

    public static SensorModel fromJson(String jsonSensorModel) {

        return gson.fromJson(jsonSensorModel, SensorModel.class);

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorModel that = (SensorModel) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
