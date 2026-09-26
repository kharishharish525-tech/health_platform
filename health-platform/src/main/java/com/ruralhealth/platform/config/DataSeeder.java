package com.ruralhealth.platform.config;

import com.ruralhealth.platform.entity.Department;
import com.ruralhealth.platform.entity.Doctor;
import com.ruralhealth.platform.entity.Product;
import com.ruralhealth.platform.repository.DepartmentRepository;
import com.ruralhealth.platform.repository.DoctorRepository;
import com.ruralhealth.platform.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a small amount of demo data (departments, doctors, billable
 * products/services) on first run, purely so the website has something to
 * show and the dropdowns aren't empty. Safe to delete this file, or it will
 * simply do nothing once these tables already have rows.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final ProductRepository productRepository;

    public DataSeeder(DepartmentRepository departmentRepository, DoctorRepository doctorRepository,
                       ProductRepository productRepository) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() == 0) {
            Department general = save(new Department(), "General Medicine", "Block A");
            Department emergency = save(new Department(), "Emergency", "Block A - Ground Floor");
            Department pediatrics = save(new Department(), "Pediatrics", "Block B");
            save(new Department(), "Diagnostics & Lab", "Block C");

            Doctor d1 = new Doctor();
            d1.setFullName("Dr. Meera Nair");
            d1.setSpecialization("General Physician");
            d1.setDepartment(general);
            doctorRepository.save(d1);

            Doctor d2 = new Doctor();
            d2.setFullName("Dr. Arjun Iyer");
            d2.setSpecialization("Emergency Medicine");
            d2.setDepartment(emergency);
            doctorRepository.save(d2);

            Doctor d3 = new Doctor();
            d3.setFullName("Dr. Kavya Menon");
            d3.setSpecialization("Pediatrician");
            d3.setDepartment(pediatrics);
            doctorRepository.save(d3);
        }

        if (productRepository.count() == 0) {
            addService("General Consultation", "Consultation", new BigDecimal("300.00"));
            addService("Emergency Consultation", "Consultation", new BigDecimal("800.00"));
            addService("Blood Test - CBC", "Diagnostics", new BigDecimal("450.00"));
            addService("X-Ray", "Diagnostics", new BigDecimal("650.00"));
            addService("ECG", "Diagnostics", new BigDecimal("500.00"));
            addGoods("Paracetamol 500mg (strip)", "Pharmacy", new BigDecimal("25.00"), 100, 20);
            addGoods("IV Fluid - Normal Saline", "Pharmacy", new BigDecimal("120.00"), 50, 15);
            addGoods("Surgical Gloves (box)", "Supplies", new BigDecimal("180.00"), 30, 10);
        }
    }

    private Department save(Department dept, String name, String location) {
        dept.setName(name);
        dept.setLocation(location);
        dept.setMonthlyBudget(BigDecimal.ZERO);
        return departmentRepository.save(dept);
    }

    private void addService(String name, String category, BigDecimal price) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setUnitPrice(price);
        p.setIsService(true);
        p.setStockQuantity(0);
        p.setReorderLevel(0);
        productRepository.save(p);
    }

    private void addGoods(String name, String category, BigDecimal price, int stock, int reorderLevel) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setUnitPrice(price);
        p.setIsService(false);
        p.setStockQuantity(stock);
        p.setReorderLevel(reorderLevel);
        productRepository.save(p);
    }
}
