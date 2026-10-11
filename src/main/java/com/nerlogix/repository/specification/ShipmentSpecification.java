package com.nerlogix.repository.specification;

import com.nerlogix.entity.Shipment;
import com.nerlogix.model.enums.CargoCategory;
import com.nerlogix.model.enums.ShipmentPriority;
import com.nerlogix.model.enums.ShipmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ShipmentSpecification {

    public static Specification<Shipment> filter(
            ShipmentStatus status,
            Long driverUserId,
            CargoCategory cargoCategory,
            ShipmentPriority priority,
            String q) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (driverUserId != null) {
                predicates.add(cb.equal(root.get("driverUser").get("id"), driverUserId));
            }

            if (cargoCategory != null) {
                predicates.add(cb.equal(root.get("cargoCategory"), cargoCategory));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Predicate codePredicate = cb.like(cb.lower(root.get("code")), pattern);
                Predicate descPredicate = cb.like(cb.lower(cb.coalesce(root.get("cargoDescription"), "")), pattern);
                predicates.add(cb.or(codePredicate, descPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
