package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        try {
            // Simulate InputReaderUtil
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            // Create a mocked ParkingSpot
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            
            // Create a mocked Ticket
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            // Simulate TicketDAO
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

            // Simulate ParkingSpotDAO
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            // Create ParkingService with mocks
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);
        
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        parkingService.processExitingVehicle(new Date());

        verify(ticketDAO, Mockito.times(1)).updateTicket(ticket);
        verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF");
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void testProcessIncomingVehicle() {
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); // Spot available
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() {
            when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        
            parkingService.processExitingVehicle(new Date());
        
            verify(ticketDAO, Mockito.times(1)).getTicket("ABCDEF");
            verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
            verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF");
            verify(parkingSpotDAO, Mockito.never()).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); // Returns spot ID 1
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR

        // Call the method to test
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Verifications
        assertNotNull(parkingSpot); // Parking should not be null
        assertEquals(1, parkingSpot.getId()); // Spot ID should be 1
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType()); // Type should be CAR
        assertTrue(parkingSpot.isAvailable()); // Spot should be available

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(inputReaderUtil, times(1)).readSelection();
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0); // No spot available
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR

        // Call the method and verify that it throws an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            parkingService.getNextParkingNumberIfAvailable();
        });

        // Verify the exception message
        assertEquals("Unexpected error while fetching parking slot", exception.getMessage());
        assertTrue(exception.getCause() instanceof Exception); // Vérifier la cause initiale de l'exception
        assertEquals("Error fetching parking number from DB. Parking slots might be full",
        exception.getCause().getMessage());

        // Verification of interactions
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(inputReaderUtil, times(1)).readSelection();
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        when(inputReaderUtil.readSelection()).thenReturn(3); // Saisie invalide (type de véhicule inexistant)

        // Call the method and verify that it throws an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            parkingService.getNextParkingNumberIfAvailable();
        });

        // Verify the exception message
        assertEquals("Entered input is invalid", exception.getMessage(), "Le message de l'exception doit correspondre");

        // Verification of interactions
        verify(inputReaderUtil, times(1)).readSelection();
        verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class));
    }

}
