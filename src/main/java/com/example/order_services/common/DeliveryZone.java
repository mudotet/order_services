package com.example.order_services.common;

import lombok.Getter;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Getter
public enum DeliveryZone {
    CAPITAL(1, Set.of("ha noi", "ho chi minh")),
    NEARBY(2, Set.of("bac ninh", "hai phong", "dong nai", "tay ninh")),
    DISTANT(3, Set.of());

    private final int shippingDays;
    private final Set<String> cities;

    DeliveryZone(int shippingDays, Set<String> cities) {
        this.shippingDays = shippingDays;
        this.cities = cities;
    }

    // ponytail: danh sách vùng cố định
    public static DeliveryZone fromCity(String city) {
        String normalized = Normalizer.normalize(city.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replaceAll("\\s+", " ");
        return Arrays.stream(values()).filter(zone -> zone.cities.contains(normalized))
                .findFirst().orElse(DISTANT);
    }
}
