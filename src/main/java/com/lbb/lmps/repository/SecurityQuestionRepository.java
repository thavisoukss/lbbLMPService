package com.lbb.lmps.repository;

import com.lbb.lmps.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, String> {

    @Query(nativeQuery = true, value =
            "SELECT sq.ID as id, sq.DESCRIPTION as description " +
            "FROM SECURITY_QUESTIONS sq " +
            "JOIN CUSTOMER_SECURITY_QUESTIONS csq ON sq.ID = csq.SECURITY_QUESTIONS_ID " +
            "WHERE csq.CUSTOMER_ID = :customerId " +
            "AND csq.DELETE_AT IS NULL " +
            "AND sq.STATUS = 'ACTIVE' AND sq.DELETE_AT IS NULL")
    List<SecurityQuestionProjection> findByCustomerId(@Param("customerId") String customerId);

    interface SecurityQuestionProjection {
        String getId();
        String getDescription();
    }

    @Query(nativeQuery = true, value =
            "SELECT csq.SECURITY_QUESTIONS_ID as questionId, csq.ANSWER as answer " +
            "FROM CUSTOMER_SECURITY_QUESTIONS csq " +
            "WHERE csq.CUSTOMER_ID = :customerId " +
            "AND csq.DELETE_AT IS NULL")
    List<CustomerAnswerProjection> findAnswersByCustomerId(@Param("customerId") String customerId);

    interface CustomerAnswerProjection {
        String getQuestionId();
        String getAnswer();
    }
}