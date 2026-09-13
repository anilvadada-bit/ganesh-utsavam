package com.ganesh.ganesh_utsavam.service;

import com.ganesh.ganesh_utsavam.entity.Donation;
import com.ganesh.ganesh_utsavam.repository.DonationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonationService {

    private final DonationRepository donationRepository;

    public DonationService(DonationRepository donationRepository) {
        this.donationRepository = donationRepository;
    }

    public Donation createDonation(Donation donation) {

        donation.setDonationDate(LocalDateTime.now());

        if (donation.getPaymentStatus() == null ||
                donation.getPaymentStatus().isBlank()) {
            donation.setPaymentStatus("PENDING");
        }

        return donationRepository.save(donation);
    }

    public List<Donation> getAllDonations() {
        return donationRepository.findAll();
    }

    public Donation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Donation not found with id: " + id));
    }
}