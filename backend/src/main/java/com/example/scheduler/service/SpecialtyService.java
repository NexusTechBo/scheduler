package com.example.scheduler.service;

import com.example.scheduler.dto.specialty.SpecialtyRequest;
import com.example.scheduler.dto.specialty.SpecialtyResponse;

import java.util.List;

public interface SpecialtyService {
    List<SpecialtyResponse> findAllSpecialties();
    SpecialtyResponse createSpecialty(SpecialtyRequest request);
}
