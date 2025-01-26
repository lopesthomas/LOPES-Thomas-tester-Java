package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        
        // Clear all database entries before each test
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
        // Simulate the entry of a vehicle
        parkingService.processIncomingVehicle();

        // Verify that a ticket is saved in the database
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Un ticket doit être enregistré dans la base de données.");
        assertEquals("ABCDEF", ticket.getVehicleRegNumber(), "The registration number should match.");

        // Verify that the parking spot availability is updated
        int nextAvailableSlot = parkingSpotDAO.getNextAvailableSlot(ticket.getParkingSpot().getParkingType());
        assertNotEquals(ticket.getParkingSpot().getId(), nextAvailableSlot, "The used parking spot should no longer be available.");
    }

    @Test
    public void testParkingLotExit(){
        // Simulate the entry of a vehicle
        testParkingACar();

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Retrieve the ticket and modify the entry time
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "The ticket must be saved in the database.");


        // Simulate the exit of a vehicle
        parkingService.processExitingVehicle(new Date(System.currentTimeMillis() + (60 * 60 * 1000)));

        // Verify that the ticket is updated with the fare and exit time
        Ticket updateTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(updateTicket.getOutTime(), "The exit time should be recorded.");
        assertTrue(updateTicket.getPrice() > 0, "The fare must be calculated and positive. Current price: " + updateTicket.getPrice());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Simulate a first entry and exit to mark the user as recurring
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle(new Date(System.currentTimeMillis()));

        // Simulate a second entry and exit
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle(new Date(System.currentTimeMillis() + (60 * 60 * 1000)));

         // Verify that the ticket shows a 5% discount for a recurring user
        Ticket updateTicket = ticketDAO.getTicket("ABCDEF");

        // Parking duration
        long durationInMilliseconds = updateTicket.getOutTime().getTime() - updateTicket.getInTime().getTime();
        double durationInHours = durationInMilliseconds / (1000.0 * 60 * 60);

        // Fare calculation with discount
        double price = durationInHours * Fare.CAR_RATE_PER_HOUR;
        double expectedPrice = price * 0.95;

        // Verify that the calculated fare matches the expected fare
        assertEquals(expectedPrice, updateTicket.getPrice(), 0.01, "The fare should include a 5% discount for a recurring user.");
    }

}
