package com.transport.assignment.repository;

import com.transport.assignment.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {
}
