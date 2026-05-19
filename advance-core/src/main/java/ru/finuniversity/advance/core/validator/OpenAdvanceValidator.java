package ru.finuniversity.advance.core.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.common.exception.OpenAdvanceExistsException;
import ru.finuniversity.advance.core.entity.Advance;
import ru.finuniversity.advance.core.repository.AdvanceRepository;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenAdvanceValidator {

    private static final List<AdvanceStatus> OPEN_STATUSES = List.of(
            AdvanceStatus.PENDING,
            AdvanceStatus.DISPATCHER_REVIEW,
            AdvanceStatus.FINANCE_REVIEW
    );

    private final AdvanceRepository advanceRepository;

    public void validate(UUID driverId, AdvanceType requestedType) {
        if (requestedType == AdvanceType.REPAIR) {
            return;
        }
        List<Advance> open = advanceRepository.findByDriverIdAndStatusIn(driverId, OPEN_STATUSES);
        if (!open.isEmpty()) {
            throw new OpenAdvanceExistsException(driverId.toString(), open.get(0).getId().toString());
        }
    }
}
