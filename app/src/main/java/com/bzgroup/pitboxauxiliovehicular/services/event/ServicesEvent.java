package com.bzgroup.pitboxauxiliovehicular.services.event;

import com.bzgroup.pitboxauxiliovehicular.entities.Address;
import com.bzgroup.pitboxauxiliovehicular.entities.Service;
import com.bzgroup.pitboxauxiliovehicular.entities.Vehicle;

import java.util.List;

public class ServicesEvent {
    public final static int SERVICES_MYVEHICLES_SUCCESS = 0;
    public final static int SERVICES_MYVEHICLES_EMPTY = 1;
    public final static int SERVICES_MYVEHICLES_ERROR = 2;

    public final static int SERVICES_SUCCESS = 3;
    public final static int SERVICES_EMPTY = 4;
    public final static int SERVICES_ERROR = 5;
    public static final int SERVICES_ADDRESS_EMPTY = 6;
    public static final int SERVICES_ADDRESS_ERROR = 7;
    public static final int SERVICES_ADDRESS_SUCCESS = 8;

    public static final int SERVICES_ADD_ADDRESS_EMPTY = 9;
    public static final int SERVICES_ADD_ADDRESS_ERROR = 10;
    public static final int SERVICES_ADD_ADDRESS_SUCCESS = 11;

    private int eventType;
    private String errorMessage;
    private List<Vehicle> myVehicles;
    private List<Service> services;
    private List<Address> addresses;

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<Vehicle> getMyVehicles() {
        return myVehicles;
    }

    public void setMyVehicles(List<Vehicle> myVehicles) {
        this.myVehicles = myVehicles;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }
}
