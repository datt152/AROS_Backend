package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.OmrAnswerDetail;

@Repository
public interface OmrAnswerDetailRepository extends JpaRepository<OmrAnswerDetail, Long> {
}
