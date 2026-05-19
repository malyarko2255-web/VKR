package ru.finuniversity.advance.common.exception;

import ru.finuniversity.advance.common.dto.AdvanceType;
import java.math.BigDecimal;

public class LimitExceededException extends AdvanceException {

    public LimitExceededException(String driverId, AdvanceType type,
                                   BigDecimal requested, BigDecimal available) {
        super(String.format(
                "Превышен лимит для водителя %s, тип %s: запрошено %.2f, доступно %.2f",
                driverId, type, requested, available),
                409, "LIMIT_EXCEEDED");
    }
}
