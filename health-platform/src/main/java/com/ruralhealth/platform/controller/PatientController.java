package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.Patient;
import com.ruralhealth.platform.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;

    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping
    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getById(@PathVariable Long id) {
        return patientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Patient create(@RequestBody Patient patient) {
        if (patient.getUhid() == null || patient.getUhid().isBlank()) {
            patient.setUhid("RH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        return patientRepository.save(patient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> update(@PathVariable Long id, @RequestBody Patient updated) {
        return patientRepository.findById(id).map(existing -> {
            updated.setPatientId(id);
            if (updated.getUhid() == null || updated.getUhid().isBlank()) {
                updated.setUhid(existing.getUhid());
            }
            return ResponseEntity.ok(patientRepository.save(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!patientRepository.existsById(id)) return ResponseEntity.notFound().build();
        patientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
