package videoeditor.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import videoeditor.db.User;

public interface UserDao extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);
}

