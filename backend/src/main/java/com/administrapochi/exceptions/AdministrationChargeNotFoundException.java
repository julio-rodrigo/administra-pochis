package com.administrapochi.exceptions;

public class AdministrationChargeNotFoundException extends RuntimeException {

    public AdministrationChargeNotFoundException(Integer id) {
        super("Administration Charge " + id + " was not found.");
    }
}
