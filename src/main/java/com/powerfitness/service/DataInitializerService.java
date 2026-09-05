package com.powerfitness.service;

import com.powerfitness.entity.*;
import com.powerfitness.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializerService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SupplementRepository supplementRepository;

    @Autowired
    private MealFoodRepository mealFoodRepository;

    @Autowired
    private UserGoalRepository userGoalRepository;

    @Autowired
    private UpiSettingRepository upiSettingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            subscriptionService.refreshAllMemberStatuses();
            return;
        }

        System.out.println("Initializing PowerFitnessKurnool database with realistic data...");

        // 1. Create Admin
        User admin = new User("admin", authService.hashPassword("admin123"), "Gym Owner / Admin", Role.ADMIN);
        userRepository.save(admin);

        // 2. Create Members
        // Member 1: Rahul Sharma (Active, 42 days left)
        User user1 = new User("9876543210", authService.hashPassword("user123"), "Rahul Sharma", Role.USER);
        userRepository.save(user1);

        Member m1 = new Member();
        m1.setMemberCode("PFK-1001");
        m1.setFullName("Rahul Sharma");
        m1.setPhoneNumber("9876543210");
        m1.setPhotoUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80");
        m1.setAdmissionDate(LocalDate.now().minusDays(48));
        m1.setSubscriptionPlan("3 Months");
        m1.setTrainingCategory("Strength Training");
        m1.setBatch("Morning Batch");
        m1.setHasCardio(false);
        m1.setTotalFee(1800.0);
        m1.setStartDate(LocalDate.now().minusDays(48));
        m1.setExpiryDate(LocalDate.now().plusDays(42));
        m1.setStatus("ACTIVE");
        m1.setNotes("Goal: Muscle Hypertrophy & Bulking. Highly dedicated.");
        m1.setUser(user1);
        m1 = memberRepository.save(m1);

        Payment p1 = new Payment();
        p1.setReceiptNumber("REC-20260715-1001");
        p1.setMember(m1);
        p1.setAmount(1800.0);
        p1.setBaseFee(1800.0);
        p1.setCardioFee(0.0);
        p1.setPaymentDate(LocalDate.now().minusDays(48));
        p1.setPaymentMethod("UPI");
        p1.setSubscriptionPlan("3 Months");
        p1.setPaymentStatus("Paid");
        p1.setTransactionRef("UPI982374182934");
        p1.setNotes("Full payment for 3 Months Strength Training");
        paymentRepository.save(p1);

        // Member 2: Suresh Reddy (Expiring Soon, 5 days left)
        User user2 = new User("9876543211", authService.hashPassword("user123"), "Suresh Reddy", Role.USER);
        userRepository.save(user2);

        Member m2 = new Member();
        m2.setMemberCode("PFK-1002");
        m2.setFullName("Suresh Reddy");
        m2.setPhoneNumber("9876543211");
        m2.setPhotoUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80");
        m2.setAdmissionDate(LocalDate.now().minusDays(25));
        m2.setSubscriptionPlan("1 Month");
        m2.setTrainingCategory("Cardio");
        m2.setBatch("Evening Batch");
        m2.setHasCardio(true);
        m2.setTotalFee(1300.0);
        m2.setStartDate(LocalDate.now().minusDays(25));
        m2.setExpiryDate(LocalDate.now().plusDays(5));
        m2.setStatus("EXPIRING_SOON");
        m2.setNotes("Needs renewal reminder call. Prefers evening cardio.");
        m2.setUser(user2);
        m2 = memberRepository.save(m2);

        Payment p2 = new Payment();
        p2.setReceiptNumber("REC-20260809-1002");
        p2.setMember(m2);
        p2.setAmount(1300.0);
        p2.setBaseFee(800.0);
        p2.setCardioFee(500.0);
        p2.setPaymentDate(LocalDate.now().minusDays(25));
        p2.setPaymentMethod("Cash");
        p2.setSubscriptionPlan("1 Month");
        p2.setPaymentStatus("Paid");
        p2.setTransactionRef("CASH-DESK-042");
        p2.setNotes("1 Month + Cardio Fee ₹500");
        paymentRepository.save(p2);

        // Member 3: Priya Varma (Expired)
        User user3 = new User("9876543212", authService.hashPassword("user123"), "Priya Varma", Role.USER);
        userRepository.save(user3);

        Member m3 = new Member();
        m3.setMemberCode("PFK-1003");
        m3.setFullName("Priya Varma");
        m3.setPhoneNumber("9876543212");
        m3.setPhotoUrl("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop&q=80");
        m3.setAdmissionDate(LocalDate.now().minusDays(200));
        m3.setSubscriptionPlan("6 Months");
        m3.setTrainingCategory("Cardio");
        m3.setBatch("Morning Batch");
        m3.setHasCardio(true);
        m3.setTotalFee(4000.0);
        m3.setStartDate(LocalDate.now().minusDays(200));
        m3.setExpiryDate(LocalDate.now().minusDays(20));
        m3.setStatus("EXPIRED");
        m3.setNotes("Subscription expired 20 days ago. Ready for renewal.");
        m3.setUser(user3);
        m3 = memberRepository.save(m3);

        Payment p3 = new Payment();
        p3.setReceiptNumber("REC-20260215-1003");
        p3.setMember(m3);
        p3.setAmount(4000.0);
        p3.setBaseFee(3500.0);
        p3.setCardioFee(500.0);
        p3.setPaymentDate(LocalDate.now().minusDays(200));
        p3.setPaymentMethod("UPI");
        p3.setSubscriptionPlan("6 Months");
        p3.setPaymentStatus("Paid");
        p3.setTransactionRef("UPI773918239012");
        p3.setNotes("6 Months + Cardio Fee");
        paymentRepository.save(p3);

        // Member 4: K. Venkat Rao (New Admission Today, 1 Year)
        User user4 = new User("9876543213", authService.hashPassword("user123"), "K. Venkat Rao", Role.USER);
        userRepository.save(user4);

        Member m4 = new Member();
        m4.setMemberCode("PFK-1004");
        m4.setFullName("K. Venkat Rao");
        m4.setPhoneNumber("9876543213");
        m4.setPhotoUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80");
        m4.setAdmissionDate(LocalDate.now());
        m4.setSubscriptionPlan("1 Year");
        m4.setTrainingCategory("Strength Training");
        m4.setBatch("Evening Batch");
        m4.setHasCardio(true);
        m4.setTotalFee(7300.0);
        m4.setStartDate(LocalDate.now());
        m4.setExpiryDate(LocalDate.now().plusDays(365));
        m4.setStatus("ACTIVE");
        m4.setNotes("Annual membership with Cardio included.");
        m4.setUser(user4);
        m4 = memberRepository.save(m4);

        Payment p4 = new Payment();
        p4.setReceiptNumber("REC-20260903-1004");
        p4.setMember(m4);
        p4.setAmount(7300.0);
        p4.setBaseFee(6800.0);
        p4.setCardioFee(500.0);
        p4.setPaymentDate(LocalDate.now());
        p4.setPaymentMethod("UPI");
        p4.setSubscriptionPlan("1 Year");
        p4.setPaymentStatus("Paid");
        p4.setTransactionRef("UPI993817264819");
        p4.setNotes("Annual subscription full payment");
        paymentRepository.save(p4);

        // Member 5: Ananya Devi (Active, 15 days left)
        User user5 = new User("9876543214", authService.hashPassword("user123"), "Ananya Devi", Role.USER);
        userRepository.save(user5);

        Member m5 = new Member();
        m5.setMemberCode("PFK-1005");
        m5.setFullName("Ananya Devi");
        m5.setPhoneNumber("9876543214");
        m5.setPhotoUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&auto=format&fit=crop&q=80");
        m5.setAdmissionDate(LocalDate.now().minusDays(15));
        m5.setSubscriptionPlan("1 Month");
        m5.setTrainingCategory("Strength Training");
        m5.setBatch("Morning Batch");
        m5.setHasCardio(false);
        m5.setTotalFee(800.0);
        m5.setStartDate(LocalDate.now().minusDays(15));
        m5.setExpiryDate(LocalDate.now().plusDays(15));
        m5.setStatus("ACTIVE");
        m5.setNotes("Morning functional training & core strength.");
        m5.setUser(user5);
        m5 = memberRepository.save(m5);

        Payment p5 = new Payment();
        p5.setReceiptNumber("REC-20260819-1005");
        p5.setMember(m5);
        p5.setAmount(800.0);
        p5.setBaseFee(800.0);
        p5.setCardioFee(0.0);
        p5.setPaymentDate(LocalDate.now().minusDays(15));
        p5.setPaymentMethod("Cash");
        p5.setSubscriptionPlan("1 Month");
        p5.setPaymentStatus("Paid");
        p5.setTransactionRef("CASH-DESK-055");
        p5.setNotes("1 Month Strength Training");
        paymentRepository.save(p5);

        // 3. User Goal setup for Rahul
        UserGoal g1 = new UserGoal(user1.getId(), "STRENGTH_TRAINING", "Lean Bulking", 2500, 160, 280, 65);
        userGoalRepository.save(g1);

        UserGoal g2 = new UserGoal(user2.getId(), "CARDIO_WEIGHT_LOSS", "Weight Loss + Strength Training", 2050, 145, 210, 50);
        userGoalRepository.save(g2);

        // 4. Seed Andhra Pradesh & Indian Meal Database
        seedMealFoods();

        // 5. Seed Supplements
        seedSupplements();

        // 6. Seed UPI Settings
        UpiSetting upi = new UpiSetting(
            "powerfitnesskurnool@okaxis",
            "Power Fitness Unisex GYM Kurnool",
            "/uploads/upi/default_upi_qr.png",
            "Scan with Google Pay, PhonePe, Paytm, or BHIM. Send screenshot to admin for instant receipt."
        );
        upiSettingRepository.save(upi);

        // 7. Seed Notifications
        notificationRepository.save(new Notification(null, "New Admission Alert", "K. Venkat Rao enrolled for 1 Year Strength + Cardio plan.", "NEW_MEMBER"));
        notificationRepository.save(new Notification(null, "Payment Received", "Payment of ₹7,300 received via UPI for Member PFK-1004.", "PAYMENT"));
        notificationRepository.save(new Notification(null, "Subscription Expiring Soon", "Member Suresh Reddy (PFK-1002) expires in 5 days.", "EXPIRY"));
        notificationRepository.save(new Notification(user2.getId(), "⚠️ Renewal Required — 5 Days Left", "Your gym membership expires in 5 days. Click here or visit the desk to renew.", "EXPIRY"));
        notificationRepository.save(new Notification(user1.getId(), "Welcome to Power Fitness Unisex GYM Kurnool!", "Build your body. Build your discipline. Track your daily nutrition in the Calories section.", "WELCOME"));

        System.out.println("PowerFitnessKurnool database initialized successfully!");
    }

    private void seedMealFoods() {
        // BREAKFAST
        mealFoodRepository.save(new MealFood(
            "Idli with Sambar", "Breakfast", "3 Idlis (150g)", 180, 6, 36, 1, 2.5,
            "Vitamin B1, B2, Niacin", "Iron, Potassium",
            "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Plain Dosa", "Breakfast", "1 Large (100g)", 165, 4, 28, 4.5, 1.8,
            "Vitamin B Complex", "Iron, Sodium",
            "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Pesarattu", "Breakfast", "1 Pesarattu (150g)", 220, 10, 32, 5, 6.0,
            "Vitamin C, Folate", "Iron, Zinc, Magnesium",
            "https://images.unsplash.com/photo-1630383249896-424e482df921?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Rava Upma", "Breakfast", "1 Bowl (180g)", 210, 5, 34, 6, 2.0,
            "Vitamin B3", "Phosphorus, Iron",
            "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Poha with Peanuts", "Breakfast", "1 Bowl (150g)", 240, 6, 38, 7, 3.0,
            "Vitamin B6, Iron", "Magnesium, Potassium",
            "https://images.unsplash.com/photo-1645177628172-a94c1f96e6db?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Medu Vada with Chutney", "Breakfast", "2 pcs (120g)", 250, 8, 26, 12, 4.0,
            "Vitamin B9", "Potassium, Calcium",
            "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Ragi Dosa", "Breakfast", "1 Dosa (120g)", 175, 5, 32, 3, 5.0,
            "Vitamin B1", "Rich Calcium, Iron",
            "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Egg Omelette (Double Egg)", "Breakfast", "2 Eggs (130g)", 190, 14, 2, 14, 0.5,
            "Vitamin B12, Vitamin D, Choline", "Selenium, Zinc",
            "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Boiled Eggs (2 Eggs)", "Breakfast", "2 Eggs (100g)", 155, 13, 1, 11, 0,
            "Vitamin B12, D, A", "Selenium, Phosphorus",
            "https://images.unsplash.com/photo-1506976785307-8732e854ad03?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Peanut Chutney", "Breakfast", "Serving (50g)", 160, 6, 6, 13, 2.5,
            "Vitamin E, Niacin", "Magnesium, Copper",
            "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?w=400&auto=format&fit=crop&q=80", true
        ));

        // LUNCH
        mealFoodRepository.save(new MealFood(
            "Andhra Sona Masoori Rice", "Lunch", "1 Bowl (200g)", 260, 5, 58, 0.8, 1.2,
            "Vitamin B1", "Iron, Potassium",
            "https://images.unsplash.com/photo-1516684732162-798a0062be99?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Brown Rice", "Lunch", "1 Bowl (200g)", 220, 5, 46, 1.8, 3.5,
            "Vitamin B1, B6", "Manganese, Magnesium",
            "https://images.unsplash.com/photo-1541832676-9b763b0239ab?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Gongura Pappu", "Lunch", "1 Cup (150g)", 195, 12, 24, 6, 7.0,
            "Vitamin A, Vitamin C, Folic Acid", "Rich Iron, Calcium, Zinc",
            "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Palakura Pappu (Spinach Dal)", "Lunch", "1 Cup (150g)", 180, 11, 22, 5, 6.0,
            "Vitamin A, Vitamin K, Folate", "Iron, Potassium",
            "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Sambar Rice", "Lunch", "1 Bowl (250g)", 290, 8, 52, 5, 4.5,
            "Vitamin B, C", "Potassium, Calcium",
            "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Tomato Rasam (Charu)", "Lunch", "1 Cup (150ml)", 45, 1.5, 8, 1, 1.0,
            "Vitamin C, Lycopene", "Potassium, Sodium",
            "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Spicy Chicken Curry (Kodi Kura)", "Lunch", "1 Serving (200g)", 320, 36, 6, 16, 1.5,
            "Vitamin B6, Vitamin B12, Niacin", "Phosphorus, Selenium",
            "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Chepala Pulusu (Fish Curry)", "Lunch", "1 Serving (220g)", 270, 30, 7, 13, 1.0,
            "Omega-3 Fatty Acids, Vitamin D", "Iodine, Selenium",
            "https://images.unsplash.com/photo-1534939561126-855b8675edd7?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Egg Curry (2 Eggs)", "Lunch", "1 Serving (180g)", 240, 15, 7, 16, 1.0,
            "Vitamin B12, Vitamin D", "Choline, Zinc",
            "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Fresh Curd / Perugu", "Lunch", "1 Cup (150g)", 90, 5, 7, 4.5, 0,
            "Vitamin B12, Riboflavin, Probiotics", "Calcium, Phosphorus",
            "https://images.unsplash.com/photo-1571212515416-fef01fc43637?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Andhra Majjiga (Spiced Buttermilk)", "Lunch", "1 Glass (250ml)", 45, 3, 4, 1.5, 0.5,
            "Probiotics, Vitamin B2", "Potassium, Calcium, Sodium",
            "https://images.unsplash.com/photo-1571212515416-fef01fc43637?w=400&auto=format&fit=crop&q=80", true
        ));

        // SNACKS
        mealFoodRepository.save(new MealFood(
            "Roasted Peanuts (Gullu)", "Snacks", "50g", 290, 13, 8, 24, 4.0,
            "Vitamin E, Niacin", "Magnesium, Manganese",
            "https://images.unsplash.com/photo-1536640712-4d4c36ff0e4e?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Roasted Chana (Putnalu)", "Snacks", "50g", 180, 11, 29, 3, 8.0,
            "Vitamin B6, Folate", "Rich Iron, Potassium",
            "https://images.unsplash.com/photo-1515543237350-b3eea1ec8082?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Sprouts Salad with Lime", "Snacks", "1 Bowl (120g)", 140, 10, 22, 1.5, 5.0,
            "Vitamin C, Vitamin K", "Iron, Zinc, Magnesium",
            "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Banana (Aratipandu)", "Snacks", "1 Large (120g)", 110, 1.2, 28, 0.3, 3.0,
            "Vitamin B6, Vitamin C", "Potassium, Magnesium",
            "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Fresh Kurnool Guava (Jaama Kaya)", "Snacks", "1 Guava (130g)", 85, 3.2, 18, 1.2, 6.5,
            "Mega Vitamin C (200% DV)", "Potassium, Folate",
            "https://images.unsplash.com/photo-1536511135898-05240c476722?w=400&auto=format&fit=crop&q=80", true
        ));
        mealFoodRepository.save(new MealFood(
            "Raw Paneer Cubes", "Snacks", "100g", 265, 18, 4, 20, 0,
            "Vitamin A, Vitamin B12", "Calcium, Phosphorus",
            "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400&auto=format&fit=crop&q=80", false
        ));

        // DINNER
        mealFoodRepository.save(new MealFood(
            "Whole Wheat Roti / Phulka", "Dinner", "2 Rotis (80g)", 160, 6, 32, 1, 4.0,
            "Vitamin B1, B3", "Iron, Magnesium",
            "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Soft Chapati", "Dinner", "2 Chapatis (90g)", 210, 6, 34, 5, 3.5,
            "Vitamin B complex", "Iron, Potassium",
            "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Dal Tadka", "Dinner", "1 Cup (150g)", 175, 10, 22, 5, 5.0,
            "Folic Acid, B vitamins", "Iron, Zinc",
            "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Grilled Chicken Breast", "Dinner", "150g", 240, 44, 0, 5, 0,
            "Vitamin B6, B12, Niacin", "Phosphorus, Selenium",
            "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Tawa Fish Fry", "Dinner", "150g", 220, 32, 3, 9, 0.5,
            "Omega-3, Vitamin D", "Iodine, Selenium",
            "https://images.unsplash.com/photo-1534939561126-855b8675edd7?w=400&auto=format&fit=crop&q=80", false
        ));
        mealFoodRepository.save(new MealFood(
            "Palak Paneer", "Dinner", "1 Bowl (180g)", 270, 15, 8, 20, 4.0,
            "Vitamin A, Vitamin K, Folate", "Rich Calcium, Iron",
            "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400&auto=format&fit=crop&q=80", false
        ));
    }

    private void seedSupplements() {
        supplementRepository.save(new Supplement(
            "Optimum Nutrition (ON) Gold Standard 100% Whey (2kg)",
            "Protein",
            6499.0,
            true,
            "PERCENTAGE",
            20.0,
            "World's #1 Selling Whey Protein. 24g pure whey protein, 5.5g BCAAs per scoop. Premium double rich chocolate flavor.",
            "https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            15
        ));
        supplementRepository.save(new Supplement(
            "MuscleBlaze Raw Whey Isolate 90% (1kg)",
            "Protein",
            2499.0,
            "Ultra-filtered whey protein isolate delivering 27g protein per 30g serving. Zero added sugar, fast absorbing for lean muscle gains.",
            "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            20
        ));
        supplementRepository.save(new Supplement(
            "MuscleBlaze Creatine Monohydrate CreAMP (250g)",
            "Creatine",
            999.0,
            true,
            "FIXED",
            200.0,
            "100% pure micronized creatine monohydrate. Enhances athletic performance, muscular power, and speeds up explosive energy.",
            "https://images.unsplash.com/photo-1546483875-ad9014c88eba?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            25
        ));
        supplementRepository.save(new Supplement(
            "Cellucor C4 Original Pre-Workout (30 Servings)",
            "Pre-Workout",
            2199.0,
            "America's #1 pre-workout powder with CarnoSyn Beta-Alanine, Creatine Nitrate, and 150mg caffeine for explosive gym pump.",
            "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            12
        ));
        supplementRepository.save(new Supplement(
            "Labrada Muscle Mass Gainer (3kg)",
            "Mass Gainer",
            3499.0,
            true,
            "FIXED",
            500.0,
            "High-calorie muscle gainer with 52g protein and 250g carbs per serving. Perfect for bulking and hardgainers.",
            "https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            10
        ));
        supplementRepository.save(new Supplement(
            "MuscleTech Platinum 100% Multivitamin (90 Tabs)",
            "Vitamins",
            1199.0,
            "Advanced daily multivitamin engineered for athletes and bodybuilders. 18 essential vitamins & minerals with amino support.",
            "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            30
        ));
        supplementRepository.save(new Supplement(
            "Fast&Up Reload Electrolytes & BCAAs (20 Tabs)",
            "Electrolytes",
            399.0,
            "Informed Choice certified instant effervescent hydration tablets. Replenishes lost minerals during intense cardio and sweat sessions.",
            "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400&auto=format&fit=crop&q=80",
            "AVAILABLE",
            50
        ));
        supplementRepository.save(new Supplement(
            "Dymatize ISO 100 Hydrolyzed Whey Protein (2.3kg)",
            "Protein",
            8299.0,
            "Scientifically proven, fast-digesting hydrolyzed 100% whey protein isolate. 25g protein, 5.5g BCAAs, under 1g fat.",
            "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?w=400&auto=format&fit=crop&q=80",
            "OUT_OF_STOCK",
            0
        ));
    }
}
