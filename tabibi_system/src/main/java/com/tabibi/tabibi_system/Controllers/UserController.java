
package com.tabibi.tabibi_system.Controllers;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tabibi.tabibi_system.Repositories.*;
import com.tabibi.tabibi_system.Services.FeedbackService;
import com.tabibi.tabibi_system.Models.Admin;
import com.tabibi.tabibi_system.Models.Booking;
import com.tabibi.tabibi_system.Models.Clinic;
import com.tabibi.tabibi_system.Models.Doctor;
import com.tabibi.tabibi_system.Models.Feedback;
import com.tabibi.tabibi_system.Models.Patient;
// import com.tabibi.tabibi_system.Models.User;
import com.tabibi.tabibi_system.Models.UserAcc;
import com.tabibi.tabibi_system.Models.UserTypePages;
import com.tabibi.tabibi_system.Models.UserTypes;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties.Http;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;

@RestController
@RequestMapping("/User")
public class UserController {
    // @Autowired
    // private UserAccRepository UserAccRepository;
    @Autowired
    UserAccRepository UserAccRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ClinicRepository clinicRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    UserTypePagesRepository page_type_repo;
    @Autowired
    UserTypeRepository user_type_repo;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JavaMailSenderImpl mailSender;
    @Autowired
    private FeedbackService feedbackService;

    // @Autowired
    // private MailSender mailSender2;

    public UserController() {
    }

    public UserController(UserAccRepository UserAccRepository) {
        this.UserAccRepository = UserAccRepository;
    }

    public UserController(UserAccRepository repo, JavaMailSenderImpl mailSender,DoctorRepository docrepo) {
        this.UserAccRepository = repo;
        this.mailSender = mailSender;
        this.doctorRepository=docrepo;
    }

    public UserAccRepository getUserAccRepository() {
        return this.UserAccRepository;
    }

    public void setUserAccRepository(UserAccRepository UserAccRepository) {
        this.UserAccRepository = UserAccRepository;
    }

    public UserController UserAccRepository(UserAccRepository UserAccRepository) {
        setUserAccRepository(UserAccRepository);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
                " UserAccRepository='" + getUserAccRepository() + "'" +
                "}";
    }




    @GetMapping("/feedback/add")
    public ModelAndView addFeedbacks(HttpSession session) {
        ModelAndView mav = admincontroller.preparenavigation(session, "addFeedbacks.html", user_type_repo,
                page_type_repo);
        Feedback feedback = new Feedback();
        String mail = (String) session.getAttribute("email");
        feedback.setMail(mail);
        mav.addObject("feedback", feedback);

        return mav;
    }
    @PostMapping("/feedback/add")
    public RedirectView addFeedback(@ModelAttribute Feedback feedback) {
        this.feedbackService.save(feedback);

        return new RedirectView("/User/feedback/add");
    }



    @GetMapping("/signup")
    public ModelAndView showSignupForm() {
        ModelAndView mav = new ModelAndView("signup.html");
        Patient patient = new Patient();
        mav.addObject("patient", patient);
        return mav;
    }

    public String hashpassword(String password) {
        String encoddedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        return encoddedPassword;
    }

    @PostMapping("signup")
    public ModelAndView processSignupForm(@Valid @ModelAttribute("patient") Patient patient, BindingResult result,
                                          @RequestParam("cpassword") String confirmPass) {
        ModelAndView signupModel = new ModelAndView("signup.html");
        ModelAndView loginModel = new ModelAndView("Login.html");
    
        if (UserAccRepository.existsByEmail(patient.getEmail())) {
            result.rejectValue("email", "error.patient", "Email already exists. Please choose a different email.");
        }
    
        if (!patient.getPass().equals(confirmPass)) {
            result.rejectValue("pass", "error.patient", "Password and Confirm Password must match.");
        }
    
        if (patient.getPass().length() < 8) {
            result.rejectValue("pass", "error.patient", "Password must be at least 8 characters long.");
        }
    
        if (patient.getPass().isEmpty()) {
            result.rejectValue("pass", "error.patient", "Password is required.");
        }
    
        if (result.hasErrors()) {
            signupModel.addObject("errors", result.getAllErrors());
            return signupModel;
        } else {
            Patient patientt = patient.getPatient();
            String encodedPassword = hashpassword(patient.getPass());
            patientt.setPass(encodedPassword);
            patientt.setUsertype(new UserTypes(4L));
            patientt.setToken("0");
            Send_Welcome_Mail(patient.getEmail());
    
            this.patientRepository.save(patientt);
        }
    
        return loginModel;
    }
    
    
public void Send_Welcome_Mail(String mail)
{         // Set up the mail sender
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("tabibii.application@gmail.com");
        mailSender.setPassword("maga ltqn qnoi azhz");


        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        // Create a mime message
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("tabibii.application@gmail.com");
            helper.setTo(mail);
            helper.setSubject("Welcome to Tabibi ");
            helper.setText("Welcome to our system , here you can book your appointments and view all the doctors you want . Manage your time with Tabibi Application ");
            // Send the mail
            mailSender.send(message);
            System.out.println("Mail sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error while sending mail.");
        }

}

    @GetMapping("/AllUsers")
    public ModelAndView getUsers() {
        ModelAndView mav = new ModelAndView("search.html");
        List<UserAcc> users = this.UserAccRepository.findAll();
        mav.addObject("users", users);
        return mav;
    }

    @GetMapping("/Login")
    public ModelAndView Login() {
        ModelAndView mav = new ModelAndView("Login.html");
        return mav;
    }

    @GetMapping("/forgetpassword")
    public ModelAndView forgetpass() {
        ModelAndView mav = new ModelAndView("forgetpassword.html");
        return mav;
    }

    public static String generateRandom4DigitNumber() {
        Random random = new Random();
        int number = 1000 + random.nextInt(9000); // Generate a random number between 1000 and 9999
        return String.valueOf(number);
    }

    public void send_token(String mail) {
        String Token = generateRandom4DigitNumber();
        // Save the token to database
        UserAcc forgetUser = this.UserAccRepository.findByEmail(mail);
        forgetUser.setToken(Token);
        this.UserAccRepository.save(forgetUser);

        // Set up the mail sender
        // JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // mailSender.setHost("smtp.gmail.com");
        // mailSender.setPort(587);
        // mailSender.setUsername("tabibii.application@gmail.com");
        // mailSender.setPassword("maga ltqn qnoi azhz");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        // Create a mime message
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("tabibii.application@gmail.com");
            helper.setTo(mail);
            helper.setSubject("Here is your reset Token :");
            helper.setText(Token);
            // Send the mail
            mailSender.send(message);
            System.out.println("Mail sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error while sending mail.");
        }

    }

    int Userid;

    @PostMapping("/forgetpassword")
    public RedirectView Sendmail(@RequestParam("email") String email) {
        if (UserAccRepository.existsByEmail(email)) {
            UserAcc forgetUser = this.UserAccRepository.findByEmail(email);
            Userid = forgetUser.getUid();
            send_token(email);

            return new RedirectView("/User/ChangePassword");
        } else {
            System.out.println("User mesh mawgood");
            return new RedirectView("/User/forgetpassword?error=UserNotFound with This Email:" + email);

        }

    }

    @GetMapping("/ChangePassword")
    public ModelAndView ChangePass() {
        return new ModelAndView("ChangePassword.html");
    }

    @PostMapping("/ChangePassword")
    public RedirectView ChangePassword(@RequestParam("NewPass") String newpass,
            @RequestParam("Confirmpass") String confirmpass, @RequestParam("Token") String Token) {
        UserAcc checkuser = this.UserAccRepository.findByUid(Userid);
        if (!newpass.equals(confirmpass)) {
            return new RedirectView("/User/ChangePassword?error=Passwords do not match ");
        } else if (Token.equals(checkuser.getToken())) {
           String hashedpassword= hashpassword(newpass);
            checkuser.setPass(hashedpassword);
            this.UserAccRepository.save(checkuser);
            return new RedirectView("/User/Savednewpass");
        } else {
            return new RedirectView("/User/ChangePassword?error=Token is incorrect ");
        }

    }

    @GetMapping("/Savednewpass")
    public ModelAndView Redirectsaved() {
        return new ModelAndView("Savedpassword.html");
    }

    @PostMapping("/test")
    public String passtest(@RequestParam("pass") String pass) {

        return pass;
    }

    @PostMapping("/Login")
    public RedirectView loginprocess(@RequestParam("email") String email, @RequestParam("pass") String pass,
            HttpSession session) {
        UserAcc newUser = this.UserAccRepository.findByEmail(email);

        if (newUser != null) {
            Boolean PasswordsMatch = BCrypt.checkpw(pass, newUser.getPass());
            if (PasswordsMatch) {
                session.setAttribute("email", newUser.getEmail());
                session.setAttribute("uid", newUser.getUid());
                session.setAttribute("usertype", newUser.getUsertype().getName());
                session.setAttribute("usertypeID", newUser.getUsertype().getUtid());

                if (newUser instanceof Patient) {
                    Patient patient = (Patient) newUser;
                    session.setAttribute("firstname", patient.getFirstname());
                    session.setAttribute("number", patient.getNumber());
                    session.setAttribute("lastname", patient.getLastname());
                    return new RedirectView("/User/patientHomepage");
                } else if (newUser instanceof Clinic) {
                    Clinic clinic = (Clinic) newUser;
                    session.setAttribute("firstname", clinic.getCname());
                    session.setAttribute("Location", clinic.getCloc());
                    session.setAttribute("number", clinic.getCnumber());
                    return new RedirectView("/User/clinicHomepage");
                } else if (newUser instanceof Doctor) {
                    Doctor doctor = (Doctor) newUser;
                    session.setAttribute("firstname", doctor.getFirstname());
                    session.setAttribute("lastname", doctor.getLastname());
                    session.setAttribute("number", doctor.getNumber());
                    session.setAttribute("specialization", doctor.getSpecialization());
                    session.setAttribute("education", doctor.getEduc());
                    return new RedirectView("/User/DoctorHomePage");
                } else if (newUser instanceof Admin) {
                    Admin admin = (Admin) newUser;
                    session.setAttribute("name", admin.getName());

                    return new RedirectView("/Admin/admin-dashboard");
                }
            } else {
                return new RedirectView("/User/Login?error=incorrectPassword" + email);
            }
        }
        return new RedirectView("/User/Login?error=userNotFound" + email);
    }

     @GetMapping("patientHomepage")
     public ModelAndView GetIndex(HttpSession session)
     {
    ModelAndView mav= admincontroller.preparenavigation(session, "patientHomepage", user_type_repo, page_type_repo);

        mav.addObject("email", (String) session.getAttribute("email"));
        mav.addObject("firstname", (String) session.getAttribute("firstname"));

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
        List<Booking> bookingList = this.bookingRepository.findByPatient(patient);
        // System.out.println("----------------------------------------------------------");
        // System.out.println("user id : " + uid);
        // System.out.println("bookings :" + bookingList.toString());
        mav.addObject("bookingList", bookingList);
        return mav;
    }

    @GetMapping("clinicHomepage")
    public ModelAndView getClinicPage(HttpSession session) {
        ModelAndView mav = admincontroller.preparenavigation(session, "ClinicHomePage.html", user_type_repo,
                page_type_repo);

        mav.addObject("email", (String) session.getAttribute("email"));
        mav.addObject("firstname", (String) session.getAttribute("firstname"));
        return mav;
    }

    @GetMapping("DoctorHomePage")
    public ModelAndView getDoctorPage(HttpSession session) {
        ModelAndView mav = admincontroller.preparenavigation(session, "DoctorHomePage", user_type_repo, page_type_repo);

        mav.addObject("email", (String) session.getAttribute("email"));
        mav.addObject("firstname", (String) session.getAttribute("firstname"));
        return mav;
    }

    @GetMapping("patients")
    public ModelAndView Getpatients(HttpSession session) {
        ModelAndView mav = admincontroller.preparenavigation(session, "patients.html", user_type_repo, page_type_repo);

        return mav;
    }

    @GetMapping("/logout")
    public RedirectView logout(HttpSession session) {
        session.invalidate();
        return new RedirectView("/User/Login");
    }

    @GetMapping("/search")
    public ModelAndView search(@RequestParam("search") String name, Model model, HttpSession session) 
    {
        List<Doctor> Doctors = doctorRepository.findByspecialization(name);
        ModelAndView mag = admincontroller.preparenavigation(session, "search.html", user_type_repo,page_type_repo);

        model.addAttribute("doctors", Doctors);
        return mag;
    }

    @PostMapping("/search")
    public ModelAndView searchresult(@RequestParam("name") String name, Model model, HttpSession session) 
    {
        List<Doctor> Doctors = doctorRepository.findByspecialization(name);
        ModelAndView mag = admincontroller.preparenavigation(session, "search.html", user_type_repo,
                page_type_repo);

        model.addAttribute("doctors", Doctors);
        return mag;
    }

    @GetMapping("/footer")
    public ModelAndView getfooter(HttpSession session) {
        ModelAndView mav = admincontroller.preparenavigation(session, "footer.html", user_type_repo, page_type_repo);

        return mav;
    }

}
