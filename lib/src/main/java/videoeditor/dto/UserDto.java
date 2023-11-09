package videoeditor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import videoeditor.db.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String email;
    private String login;
    private String token;

    public UserDto(User user) {
    	id = user.getId();
    	email = user.getEmail();
    	login = user.getLogin();
    }
    
}