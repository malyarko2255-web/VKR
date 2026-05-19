package ru.finuniversity.advance.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.core.entity.Advance;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdvanceRepository extends JpaRepository<Advance, UUID>,
        JpaSpecificationExecutor<Advance> {

    List<Advance> findByDriverIdAndStatusIn(UUID driverId, Collection<AdvanceStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(a.amount), 0)
            FROM Advance a
            WHERE a.driverId = :driverId
              AND a.advanceType = :type
              AND a.status IN :statuses
            """)
    BigDecimal sumAmountByDriverAndTypeAndStatusIn(
            @Param("driverId") UUID driverId,
            @Param("type") AdvanceType type,
            @Param("statuses") Collection<AdvanceStatus> statuses);
}
