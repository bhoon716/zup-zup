package bhoon.sugang_helper.identity.user.controller;

import bhoon.sugang_helper.identity.user.dto.command.RegisterDeviceCommand;
import bhoon.sugang_helper.identity.user.dto.request.UserDeviceRequest;
import bhoon.sugang_helper.identity.user.dto.request.UserDeviceUnregisterRequest;
import bhoon.sugang_helper.identity.user.dto.response.UserDeviceResponse;
import bhoon.sugang_helper.identity.user.service.UserDeviceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @GetMapping
    public ResponseEntity<List<UserDeviceResponse>> getUserDevices() {
        List<UserDeviceResponse> responses = userDeviceService.getUserDevices(
                        userDeviceService.getCurrentUserOrThrow().getId())
                .stream()
                .map(UserDeviceResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody UserDeviceRequest request) {
        userDeviceService.registerDevice(new RegisterDeviceCommand(
                request.getType(),
                request.getToken(),
                request.getP256dh(),
                request.getAuth(),
                request.getAlias()));
        return ResponseEntity.ok(null);
    }

    @DeleteMapping
    public ResponseEntity<Void> unregisterDevice(
            @Valid @RequestBody UserDeviceUnregisterRequest request) {
        userDeviceService.unregisterDevice(request.token());
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        userDeviceService.deleteDeviceById(id);
        return ResponseEntity.ok(null);
    }

    @Deprecated(since = "2026-07-13", forRemoval = true)
    @DeleteMapping("/token/{token}")
    public ResponseEntity<Void> unregisterDevice(@PathVariable String token) {
        userDeviceService.unregisterDevice(token);
        return ResponseEntity.ok(null);
    }
}
