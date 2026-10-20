package com.example.scheduler.util;

import com.example.scheduler.dto.patient.PatientRequest;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

public final class Utils {

    private Utils() {}

    public static String subdomainFor(String clinicName) {
        var slug = Normalizer.normalize(clinicName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return "@" + slug + ".com";
    }
}
