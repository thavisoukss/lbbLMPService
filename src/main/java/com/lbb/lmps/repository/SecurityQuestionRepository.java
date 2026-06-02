package com.lbb.lmps.repository;

import com.lbb.lmps.entity.CustomerSecurityQuestion;
import com.lbb.lmps.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, String> {

    @Query("SELECT sq.id as id, sq.description as description " +
            "FROM SecurityQuestion sq " +
            "JOIN CustomerSecurityQuestion csq ON sq.id = csq.securityQuestionsId " +
            "WHERE csq.customerId = :customerId " +
            "AND csq.deleteAt IS NULL " +
            "AND sq.status = 'ACTIVE' AND sq.deleteAt IS NULL")
    List<SecurityQuestionProjection> findByCustomerId(@Param("customerId") String customerId);

    interface SecurityQuestionProjection {
        String getId();
        String getDescription();
    }

    @Query("SELECT csq.securityQuestionsId as questionId, csq.answer as answer " +
            "FROM CustomerSecurityQuestion csq " +
            "WHERE csq.customerId = :customerId " +
            "AND csq.deleteAt IS NULL")
    List<CustomerAnswerProjection> findAnswersByCustomerId(@Param("customerId") String customerId);

    interface CustomerAnswerProjection {
        String getQuestionId();
        String getAnswer();
    }
}