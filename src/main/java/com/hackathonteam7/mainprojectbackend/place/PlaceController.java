package com.hackathonteam7.mainprojectbackend.place;

import com.hackathonteam7.mainprojectbackend.place.dto.PlaceCreateRequest;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceDetailResponse;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceResponse;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceUpdateRequest;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public List<PlaceResponse> list(@AuthenticationPrincipal PrincipalUser me,
                                     @RequestParam(name = "region_id", required = false) Long regionId) {
        return placeService.list(me.orgId(), regionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse create(@AuthenticationPrincipal PrincipalUser me, @Valid @RequestBody PlaceCreateRequest request) {
        return placeService.create(me.orgId(), request);
    }

    @GetMapping("/{id}")
    public PlaceDetailResponse getDetail(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        return placeService.getDetail(me.orgId(), id);
    }

    @PutMapping("/{id}")
    public PlaceResponse update(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id,
                                 @Valid @RequestBody PlaceUpdateRequest request) {
        return placeService.update(me.orgId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        placeService.delete(me.orgId(), id);
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrcode(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        byte[] png = placeService.qrcodePng(me.orgId(), id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(86400)).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
