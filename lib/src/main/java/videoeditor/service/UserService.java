package videoeditor.service;


import java.nio.CharBuffer;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import videoeditor.dao.UserDao;
import videoeditor.db.User;
import videoeditor.dto.CredentialsDto;
import videoeditor.dto.SignUpDto;
import videoeditor.dto.UserDto;
import videoeditor.exceptions.AppException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

	@Autowired
    private UserDao userDao;

	@Autowired
    private PasswordEncoder passwordEncoder;

    public UserDto login(CredentialsDto credentialsDto) {
        User user = userDao.findByLogin(credentialsDto.login())
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));

        if (passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()), user.getPassword())) {
            return new UserDto(user); 
        }
        throw new AppException("Invalid password", HttpStatus.BAD_REQUEST);
    }
    
    public UserDto register(SignUpDto userDto) {
        Optional<User> optionalUser = userDao.findByLogin(userDto.login());

        if (optionalUser.isPresent()) {
            throw new AppException("Login already exists", HttpStatus.BAD_REQUEST);
        }

        User user = new User(userDto);
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        User savedUser = userDao.save(user);

        return new UserDto(savedUser);
    }
    
    public UserDto findByLogin(String login) {
        User user = userDao.findByLogin(login)
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));
        return new UserDto(user);
    }

}