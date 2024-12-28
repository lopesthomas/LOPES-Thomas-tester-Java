package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inTimeHour = ticket.getInTime().getTime();
        long outTimeHour = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        double duration = (outTimeHour - inTimeHour) / (1000.0 * 60 * 60);

        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive: " + duration);
        }

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                System.out.println("Price calculated for CAR: " + ticket.getPrice());
                break;
            }
            case BIKE: {
                ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                System.out.println("Price calculated for BIKE: " + ticket.getPrice());
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}