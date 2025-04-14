package ru.maslov.trucknavigator.dto.compliance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO с результатами проверки соответствия нормативам.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceResultDto {

    private boolean compliant;
    private List<String> warnings = new ArrayList<>();
}