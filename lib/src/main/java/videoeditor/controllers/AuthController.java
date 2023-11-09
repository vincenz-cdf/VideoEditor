package videoeditor.controllers;

import lombok.RequiredArgsConstructor;
import videoeditor.config.UserAuthProvider;
import videoeditor.dto.CredentialsDto;
import videoeditor.dto.SignUpDto;
import videoeditor.dto.UserDto;
import videoeditor.service.UserService;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AuthController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private UserAuthProvider userAuthProvider;
	
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody CredentialsDto credentialsDto) {
        UserDto userDto = userService.login(credentialsDto);
        userDto.setToken(userAuthProvider.createToken(userDto));
        return ResponseEntity.ok(userDto);
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody SignUpDto user) {
        UserDto createdUser = userService.register(user);
        createdUser.setToken(userAuthProvider.createToken(createdUser));
        return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
    }
}
