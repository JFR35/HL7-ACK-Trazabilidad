package com.myobservation.storage.repository;

import com.myobservation.storage.model.entity.HL7Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HL7MessageRepository extends JpaRepository<HL7Message, Long> {
    /**
     * Buscar por el código de estado ACK
     * @param ackStatus AA AE AR
     * @return código de estado
     */
    List<HL7Message> findByAckStatusIn(List<String> ackStatus);
}