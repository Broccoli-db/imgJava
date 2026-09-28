package com.imgjava.demo.repository;

import com.imgjava.demo.domain.DemoNote;
import org.springframework.data.jpa.repository.JpaRepository;

/** 演示用数据访问接口，见 {@link com.imgjava.demo.domain.DemoNote}。 */
public interface DemoNoteRepository extends JpaRepository<DemoNote, Long> {
}
