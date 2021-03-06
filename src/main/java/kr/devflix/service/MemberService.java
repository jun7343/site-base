package kr.devflix.service;

import com.google.common.collect.ImmutableList;
import kr.devflix.constant.MemberConfirmType;
import kr.devflix.constant.MemberStatus;
import kr.devflix.constant.RoleType;
import kr.devflix.entity.Member;
import kr.devflix.entity.MemberConfirm;
import kr.devflix.repository.MemberConfirmRepository;
import kr.devflix.repository.MemberRepository;
import kr.devflix.utils.JavaMailUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final MemberConfirmRepository memberConfirmRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailUtils javaMailUtils;

    public MemberService(MemberRepository memberRepository, MemberConfirmRepository memberConfirmRepository,
                         PasswordEncoder passwordEncoder, JavaMailUtils javaMailUtils) {
        this.memberRepository = memberRepository;
        this.memberConfirmRepository = memberConfirmRepository;
        this.passwordEncoder = passwordEncoder;
        this.javaMailUtils = javaMailUtils;
    }

    @Transactional
    public Member createMemberAndDeleteMemberConfirm(final String email, final String password, final String username) {
        final Member user = memberRepository.findByEmail(email);

        if (user != null) {
            return null;
        }

        Member joinUser = Member.builder()
                .email(email)
                .status(MemberStatus.ACTIVE)
                .password(passwordEncoder.encode(password))
                .username(username)
                .authority(ImmutableList.of(RoleType.USER))
                .createAt(new Date())
                .updateAt(new Date())
                .build();

        final MemberConfirm confirm = memberConfirmRepository.findByEmailEquals(email);

        if (confirm != null) {
            memberConfirmRepository.delete(confirm);
        }

        return memberRepository.save(joinUser);
    }

    @Transactional
    public Member findUserByEmail(final String email) {
        return memberRepository.findByEmail(email);
    }

    @Transactional
    public MemberConfirm createOrUpdateMemberConfirmByEmail(final String email, final MemberConfirmType type, HttpServletRequest request) {
        MemberConfirm findConfirm = memberConfirmRepository.findByEmailEquals(email);
        UUID uuid = UUID.randomUUID();
        MemberConfirm newConfirm;

        if (findConfirm == null) {
            newConfirm = MemberConfirm.builder()
                    .type(type)
                    .email(email)
                    .confirmCount(1)
                    .uuid(uuid.toString())
                    .createAt(new Date())
                    .updateAt(new Date())
                    .build();
        } else {
            newConfirm = MemberConfirm.builder()
                    .id(findConfirm.getId())
                    .type(type)
                    .email(findConfirm.getEmail())
                    .confirmCount(findConfirm.getConfirmCount() + 1)
                    .uuid(uuid.toString())
                    .createAt(findConfirm.getCreateAt())
                    .updateAt(new Date())
                    .build();
        }

        if (newConfirm.getConfirmCount() <= 5) {
            if (type == MemberConfirmType.EMAIL_AUTHENTICATION) {
                javaMailUtils.emailConfirmSendMail(newConfirm, uuid, request);
            } else if (type == MemberConfirmType.PASSWORD) {
                javaMailUtils.findPasswordSendMail(newConfirm, uuid, request);
            }

            return memberConfirmRepository.save(newConfirm);
        } else {
            return newConfirm;
        }
    }

    @Transactional
    public MemberConfirm findMemberConfirmByEmail(final String email) {
        return memberConfirmRepository.findByEmailEquals(email);
    }

    @Transactional
    public MemberConfirm findMemberConfirmByUuid(final String uuid) {
        return memberConfirmRepository.findByUuidEquals(uuid);
    }

    @Transactional
    public Member updateMemberPasswordAndDeleteMemberConfirm(final String password, final MemberConfirm confirm) {
        final Member user = memberRepository.findByEmail(confirm.getEmail());

        final Member updateUser = Member.builder()
                .id(user.getId())
                .password(passwordEncoder.encode(password))
                .email(user.getEmail())
                .status(user.getStatus())
                .username(user.getUsername())
                .authority(user.getAuthority())
                .createAt(user.getCreateAt())
                .updateAt(new Date())
                .build();

        memberConfirmRepository.delete(confirm);

        return memberRepository.save(updateUser);
    }

    @Transactional
    public void updateMemberInfo(final Member member) {
        if (member != null && member.getId() != null) {
            memberRepository.save(member);
        }
    }

    @Override
    public Member loadUserByUsername(final String email) throws UsernameNotFoundException {
        final Member user = memberRepository.findByEmailEquals(email);

        if (user == null) {
            throw new InternalAuthenticationServiceException("user not found");
        }

        return user;
    }

    @Transactional
    public Page<Member> findAll(final int page, final int size) {
        return memberRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createAt"))));
    }

    @Transactional
    public Page<Member> findAllBySearch(final String email, MemberStatus status,
                                        final int page, final int size) {
        return memberRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> list = new LinkedList<>();

            if (! StringUtils.isBlank(email)) {
                list.add(criteriaBuilder.equal(root.get("email"), email));
            }

            if (status != null) {
                list.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(list.toArray(new Predicate[0]));
        }, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createAt"))));
    }

    @Transactional
    public Optional<Member> findOneById(long id) {
        return memberRepository.findById(id);
    }
}
