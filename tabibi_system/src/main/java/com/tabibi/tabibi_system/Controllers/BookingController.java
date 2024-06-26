package com.tabibi.tabibi_system.Controllers;

import com.tabibi.tabibi_system.Models.Appointment;
import com.tabibi.tabibi_system.Models.Booking;
import com.tabibi.tabibi_system.Models.Patient;
import com.tabibi.tabibi_system.Repositories.AppointmentRepository;
import com.tabibi.tabibi_system.Repositories.BookingRepository;
import com.tabibi.tabibi_system.Repositories.DoctorRepository;
import com.tabibi.tabibi_system.Repositories.PagesRepository;
import com.tabibi.tabibi_system.Repositories.PatientRepository;
import com.tabibi.tabibi_system.Repositories.UserTypePagesRepository;
import com.tabibi.tabibi_system.Repositories.UserTypeRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
public class BookingController {
    @Autowired
    UserTypeRepository user_type_repo;
    @Autowired
    public UserTypePagesRepository page_type_repo;
    @Autowired
    PagesRepository pages_repo;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BookingRepository bookingRepository;

 @GetMapping("")
    public ModelAndView viewAppointmentsForBooking(@RequestParam(required = false) String specialization, HttpSession session) {
        ModelAndView mav= admincontroller.preparenavigation(session, "booking", user_type_repo, page_type_repo);

        List<String> uniqueSpecializations = doctorRepository.findDistinctSpecializationsByAppointments();
        List<Appointment> appointmentList;

        // Retrieve the user ID from the session
        Object uidObject = session.getAttribute("uid");
        long uid;
        if (uidObject instanceof Integer) {
            uid = ((Integer) uidObject).longValue();
        } else if (uidObject instanceof Long) {
            uid = (Long) uidObject;
        } else {
            throw new IllegalStateException("Invalid user ID type");
        }

        // Fetch the patient's booked appointments
        Patient patient = this.patientRepository.findByUid(uid);
        List<Booking> userBookings = bookingRepository.findByPatient(patient);
        
        // Extract appointment IDs from the user's bookings
        List<Long> bookedAppointmentIds = userBookings.stream()
                .map(Booking::getAppointment)
                .map(Appointment::getAppId)
                .collect(Collectors.toList());

        // Fetch and filter available appointments
        if (specialization != null && !specialization.isEmpty()) {
            appointmentList = appointmentRepository.findByDoctorSpecializationAndStatus(specialization, "available")
                    .stream()
                    .filter(appointment -> !bookedAppointmentIds.contains(appointment.getAppId()))
                    .collect(Collectors.toList());
        } else {                                                                                                  //
            appointmentList = appointmentRepository.findByStatus("available")
                    .stream()
                    .filter(appointment -> !bookedAppointmentIds.contains(appointment.getAppId()))
                    .collect(Collectors.toList());
        }


  // userBookings.stream() - Creates a stream from the list of bookings.
        // .map(Booking::getAppointment) - Transforms each Booking to its associated Appointment.
        // .map(Appointment::getAppId) - Transforms each Appointment to its ID (appId).
        // .collect(Collectors.toList()) - Collects all the appointment IDs into a list (bookedAppointmentIds).


        // Update the status of appointments with no available places
        appointmentList.forEach(appointment -> {
            if (appointment.getPlacesLeft() == 0) {
                appointment.setStatus("reserved");
                appointmentRepository.save(appointment);
            }
        });

        // Fetch the updated list of appointments after status update
        if (specialization != null && !specialization.isEmpty()) {
            appointmentList = appointmentRepository.findByDoctorSpecializationAndStatus(specialization, "available")
                    .stream()
                    .filter(appointment -> !bookedAppointmentIds.contains(appointment.getAppId()))
                    .collect(Collectors.toList());
        } else {
            appointmentList = appointmentRepository.findByStatus("available")
                    .stream()
                    .filter(appointment -> !bookedAppointmentIds.contains(appointment.getAppId()))
                    .collect(Collectors.toList());
        }

        mav.addObject("appointmentList", appointmentList);
        mav.addObject("uniqueSpecializations", uniqueSpecializations);

        return mav;
    }

    @PostMapping("/bookAppointment")
    public RedirectView bookAppointment(@RequestParam long appointmentId, HttpSession session) {
        System.out.println("-------------------- BOOKING DETAILS");

        Appointment selectedAppointment = this.appointmentRepository.findByappId(appointmentId);

        if (selectedAppointment.getPlacesLeft() <= 0) {
            System.out.println("No available slots for this appointment");
            return new RedirectView("/booking?error=no_slots_available");
        }

        Object uidObject = session.getAttribute("uid");
        long uid;
        if (uidObject instanceof Integer) {
            uid = ((Integer) uidObject).longValue();
        } else if (uidObject instanceof Long) {
            uid = (Long) uidObject;
        } else {
            throw new IllegalStateException("Invalid user ID type");
        }
        Patient patient = this.patientRepository.findByUid(uid);
        Booking bookedAppointment = new Booking(selectedAppointment, patient);
        this.bookingRepository.save(bookedAppointment);
        selectedAppointment.setBooked(selectedAppointment.getBooked() + 1);
        this.appointmentRepository.save(selectedAppointment);


        return new RedirectView("/booking");
    }


    @PostMapping("/cancelBooking")
    public RedirectView cancelBooking(@RequestParam long bookingId){
this.bookingRepository.deleteById(bookingId);
   return new RedirectView("/User/patientHomepage");

    }



}
