package com.myobservation.storage.repository;

import com.myobservation.storage.model.entity.HL7Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HL7MessageRepository extends JpaRepository<HL7Message, Long> {

}