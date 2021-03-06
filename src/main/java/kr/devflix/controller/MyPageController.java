package kr.devflix.controller;

import kr.devflix.constant.MemberStatus;
import kr.devflix.constant.RoleType;
import kr.devflix.constant.Status;
import kr.devflix.entity.Member;
import kr.devflix.entity.Post;
import kr.devflix.entity.PostComment;
import kr.devflix.service.MemberService;
import kr.devflix.service.PostCommentService;
import kr.devflix.service.PostService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class MyPageController {

    private final MemberService memberService;
    private final PostService postService;
    private final PostCommentService postCommentService;
    private final PasswordEncoder passwordEncoder;
    private final int DEFAULT_SIZE_VALUE = 20;

    public MyPageController(MemberService memberService, PostService postService, PostCommentService postCommentService, PasswordEncoder passwordEncoder) {
        this.memberService = memberService;
        this.postService = postService;
        this.postCommentService = postCommentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page", method = RequestMethod.GET)
    public String myPageForm(@AuthenticationPrincipal Member user, Model model) {
        model.addAttribute("item", user);

        return "/my-page/index";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page", method = RequestMethod.POST)
    public String myPageAction(@RequestParam(name = "id", required = false)final long id,
                               @RequestParam(name = "username", required = false)final String username,
                               @RequestParam(name = "description", required = false)final String description,
                               @RequestParam(name = "path-base", required = false)final String pathBase,
                               @RequestParam(name = "image", required = false)final String image,
                               @RequestParam(name = "github", required = false)final String github,
                               @RequestParam(name = "facebook", required = false)final String facebook,
                               @RequestParam(name = "twitter", required = false)final String twitter,
                               @RequestParam(name = "instagram", required = false)final String instagram,
                               @RequestParam(name = "linked-in", required = false)final String linkedIn,
                               @AuthenticationPrincipal Member user, RedirectAttributes attrs) {
        if (user.getId().equals(id)) {
            if (StringUtils.isBlank(username)) {
                attrs.addFlashAttribute("errorMessage", "유저 이름 기입해 주세요.");

                return "redirect:/my-page";
            }

            memberService.updateMemberInfo(Member.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .password(user.getPassword())
                            .username(username)
                            .status(user.getStatus())
                            .description(description)
                            .github(github)
                            .facebook(facebook)
                            .twiter(twitter)
                            .instagram(instagram)
                            .linkedIn(linkedIn)
                            .pathBase(pathBase)
                            .imagePath(image)
                            .authority(user.getAuthority())
                            .createAt(user.getCreateAt())
                            .updateAt(new Date())
                            .build());

            // Security Context에 업데이트한 유저 정보 반영
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            final Member updateMember = memberService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(updateMember, authentication.getCredentials(), updateMember.getAuthorities());
            token.setDetails(authentication.getDetails());

            SecurityContextHolder.getContext().setAuthentication(token);
        } else {
            attrs.addFlashAttribute("errorMessage", "오류가 발생 하였습니다.");
        }

        return "redirect:/my-page";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page/password", method = RequestMethod.GET)
    public String myPagePasswordForm() {
        return "/my-page/password";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page/password", method = RequestMethod.POST)
    public String myPagePasswordAction(@RequestParam(name = "current-password", required = false)final String currentPassword,
                                       @RequestParam(name = "new-password", required = false)final String newPassword,
                                       @AuthenticationPrincipal Member user, RedirectAttributes attrs) {
        if (StringUtils.isBlank(currentPassword)) {
            attrs.addFlashAttribute("errorMessage", "현재 비밀번호 기입해 주세요.");

            return "redirect:/my-page/password";
        } else if (StringUtils.isBlank(newPassword)) {
            attrs.addFlashAttribute("errorMessage", "새로운 비밀번호 기입해 주세요.");

            return "redirect:/my-page/password";
        } else if (! passwordEncoder.matches(currentPassword, user.getPassword())) {
            attrs.addFlashAttribute("errorMessage", "현재 비밀번호와 맞지 않습니다.");

            return "redirect:/my-page/password";
        }

        memberService.updateMemberInfo(Member.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .password(passwordEncoder.encode(newPassword))
                        .username(user.getUsername())
                        .status(user.getStatus())
                        .description(user.getDescription())
                        .github(user.getGithub())
                        .facebook(user.getFacebook())
                        .twiter(user.getTwiter())
                        .instagram(user.getInstagram())
                        .linkedIn(user.getLinkedIn())
                        .pathBase(user.getPathBase())
                        .imagePath(user.getImagePath())
                        .authority(user.getAuthority())
                        .createAt(user.getCreateAt())
                        .updateAt(new Date())
                        .build());

        SecurityContextHolder.clearContext();
        attrs.addFlashAttribute("successMessage", "비밀번호 변경이 완료 되었습니다. 재로그인해 주세요.");

        return "redirect:/login";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page/withdrawal", method = RequestMethod.GET)
    public String myPageWithdrawlForm() {
        return "/my-page/withdrawal";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page/withdrawal", method = RequestMethod.POST)
    public String myPAgeWithdrawlAction(@RequestParam(name = "email", required = false)final String email,
                                        @RequestParam(name = "password", required = false)final String password,
                                        @AuthenticationPrincipal Member user, RedirectAttributes attrs) {
        if (StringUtils.isBlank(email)) {
            attrs.addFlashAttribute("errorMessage", "이메일 기입해 주세요.");

            return "redirect:/my-page/withdrawl";
        } else if (StringUtils.isBlank(password)) {
            attrs.addFlashAttribute("errorMessage", "패스워드 기입해 주세요.");

            return "redirect:/my-page/withdrawl";
        } else if (! StringUtils.equals(user.getEmail(), email)) {
            attrs.addFlashAttribute("errorMessage", "이메일이 맞지 않습니다.");

            return "redirect:/my-page/withdrawal";
        } else if (! passwordEncoder.matches(password, user.getPassword())) {
            attrs.addFlashAttribute("errorMessage", "비밀번호가 맞지 않습니다.");

            return "redirect:/my-page/withdrawal";
        }

        memberService.updateMemberInfo(Member.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .username(user.getUsername())
                .status(MemberStatus.WITHDRAWAL)
                .description(user.getDescription())
                .github(user.getGithub())
                .facebook(user.getFacebook())
                .twiter(user.getTwiter())
                .instagram(user.getInstagram())
                .linkedIn(user.getLinkedIn())
                .pathBase(user.getPathBase())
                .imagePath(user.getImagePath())
                .authority(user.getAuthority())
                .createAt(user.getCreateAt())
                .updateAt(new Date())
                .build());

        postService.updateAllStatusByWriter(Status.DELETE, user);

        SecurityContextHolder.clearContext();
        attrs.addFlashAttribute("successMessage", "정상적으로 회원 탈퇴 되었습니다.");

        return "redirect:/login";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page/post-write", method = RequestMethod.GET)
    public String myPagePostWriteList(@RequestParam(name = "page", required = false, defaultValue = "0")final int page,
                                      @AuthenticationPrincipal Member user, Model model) {
        Page<Post> findList = postService.findAllByWriterAndStatusAndPageRequest(user, Status.POST, page, DEFAULT_SIZE_VALUE);
        List<Integer> pageNumList = new ArrayList<>();

        model.addAttribute("list", findList);

        if (findList.getNumber() / 5 != 0 && ((findList.getNumber() / 5) * 5 - 1) > 0) {
            model.addAttribute("previousPageNum", (findList.getNumber() / 5) * 5 - 1);
        }

        if ((findList.getNumber() / 5) * 5 + 6 <= findList.getTotalPages()) {
            model.addAttribute("nextPageNum", (findList.getNumber() / 5 + 1) * 5);
        }

        int start = (findList.getNumber() / 5) * 5 + 1;
        int end = Math.min((findList.getNumber() / 5 + 1) * 5, findList.getTotalPages());

        for (int i = start; i <= end; i++) {
            pageNumList.add(i);
        }

        model.addAttribute("pageNumList", pageNumList);
        model.addAttribute("currentPageNum", findList.getNumber() + 1);
        model.addAttribute("pagination", findList.getTotalPages() > 1);


        return "/my-page/post-write";
    }

    @Secured(RoleType.USER)
    @RequestMapping(path = "/my-page/post-comment", method = RequestMethod.GET)
    public String myPagePostCommentList(@RequestParam(name = "page", required = false, defaultValue = "0")final int page,
                                        @AuthenticationPrincipal Member user, Model model) {
        Page<PostComment> findList = postCommentService.findAllByWriterAndStatusAndPageRequest(user, Status.POST, page, DEFAULT_SIZE_VALUE);
        List<Integer> pageNumList = new ArrayList<>();

        model.addAttribute("list", findList);

        if (findList.getNumber() / 5 != 0 && ((findList.getNumber() / 5) * 5 - 1) > 0) {
            model.addAttribute("previousPageNum", (findList.getNumber() / 5) * 5 - 1);
        }

        if ((findList.getNumber() / 5) * 5 + 6 <= findList.getTotalPages()) {
            model.addAttribute("nextPageNum", (findList.getNumber() / 5 + 1) * 5);
        }

        int start = (findList.getNumber() / 5) * 5 + 1;
        int end = Math.min((findList.getNumber() / 5 + 1) * 5, findList.getTotalPages());

        for (int i = start; i <= end; i++) {
            pageNumList.add(i);
        }

        model.addAttribute("pageNumList", pageNumList);
        model.addAttribute("currentPageNum", findList.getNumber() + 1);
        model.addAttribute("pagination", findList.getTotalPages() > 1);

        return "/my-page/post-comment";
    }
}
