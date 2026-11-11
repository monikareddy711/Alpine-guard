package io.alpineguard.api;

import io.alpineguard.domain.AvalancheBulletin;
import io.alpineguard.repository.BulletinRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bulletins")
@Tag(name = "bulletins", description = "Avalanche bulletins per region and elevation band")
public class BulletinController {

    private final BulletinRepository bulletinRepository;

    public BulletinController(BulletinRepository bulletinRepository) {
        this.bulletinRepository = bulletinRepository;
    }

    @GetMapping
    @Operation(summary = "Newest bulletins, optionally filtered by region")
    public List<AvalancheBulletin> latest(@RequestParam(required = false) String region) {
        List<AvalancheBulletin> bulletins = bulletinRepository.latest();
        if (region == null || region.isBlank()) {
            return bulletins;
        }
        return bulletins.stream().filter(bulletin -> bulletin.region().equalsIgnoreCase(region)).toList();
    }
}
