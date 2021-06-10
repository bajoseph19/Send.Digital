package com.varscon.sendcorp.SendCorp.accounts.repositories;

import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserModel, String> {

    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByPhone(String phone);

}