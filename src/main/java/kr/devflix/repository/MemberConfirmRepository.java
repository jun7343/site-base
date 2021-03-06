package kr.devflix.repository;

import kr.devflix.entity.MemberConfirm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberConfirmRepository extends JpaRepository<MemberConfirm, Long> {
    MemberConfirm findByEmailEquals(String email);

    MemberConfirm findByUuidEquals(String uuid);
}
