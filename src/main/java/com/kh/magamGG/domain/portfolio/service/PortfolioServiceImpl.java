package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.portfolio.dto.*;
import com.kh.magamGG.domain.portfolio.entity.Portfolio;
import com.kh.magamGG.domain.portfolio.repository.PortfolioRepository;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private static final String STATUS_ACTIVE = "Y";
    private static final String STATUS_INACTIVE = "N";

    private final PortfolioRepository portfolioRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotionPortfolioSyncService notionPortfolioSyncService;

    @Override
    @Transactional
    public PortfolioResponse createFromExtract(Long memberNo, PortfolioExtractDto extractDto) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        String career = joinList(extractDto.careerItems());
        String project = joinList(extractDto.projects());
        String skill = joinList(extractDto.skills());
        if (extractDto.role() != null && !extractDto.role().isBlank() && (career == null || career.isBlank())) {
            career = extractDto.role();
        } else if (extractDto.role() != null && !extractDto.role().isBlank()) {
            career = extractDto.role() + "\n" + (career != null ? career : "");
        }
        Portfolio portfolio = Portfolio.builder()
                .member(member)
                .portfolioUserName(emptyToNull(extractDto.name()))
                .portfolioUserPhone(emptyToNull(extractDto.phone()))
                .portfolioUserEmail(emptyToNull(extractDto.email()))
                .portfolioUserCareer(emptyToNull(career))
                .portfolioUserProject(emptyToNull(project))
                .portfolioUserSkill(emptyToNull(skill))
                .portfolioStatus(STATUS_ACTIVE)
                .build();
        portfolio = portfolioRepository.save(portfolio);
        return toResponse(portfolio);
    }

    @Override
    @Transactional
    public PortfolioResponse create(Long memberNo, PortfolioCreateRequest request) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Portfolio portfolio = Portfolio.builder()
                .member(member)
                .portfolioUserName(emptyToNull(request.getPortfolioUserName()))
                .portfolioUserPhone(emptyToNull(request.getPortfolioUserPhone()))
                .portfolioUserEmail(emptyToNull(request.getPortfolioUserEmail()))
                .portfolioUserCareer(emptyToNull(request.getPortfolioUserCareer()))
                .portfolioUserProject(emptyToNull(request.getPortfolioUserProject()))
                .portfolioUserSkill(emptyToNull(request.getPortfolioUserSkill()))
                .portfolioStatus(STATUS_ACTIVE)
                .build();
        portfolio = portfolioRepository.save(portfolio);
        return toResponse(portfolio);
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getMyPortfolio(Long memberNo) {
        return portfolioRepository.findFirstByMember_MemberNoAndPortfolioStatusOrderByPortfolioCreatedAtDesc(memberNo, STATUS_ACTIVE)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistProjectItemDto> getMyProjectsForForm(Long memberNo) {
        List<ProjectMember> list = projectMemberRepository.findByMember_MemberNo(memberNo);
        return list.stream()
                .map(pm -> {
                    Project p = pm.getProject();
                    return ArtistProjectItemDto.builder()
                            .projectNo(p.getProjectNo())
                            .projectName(p.getProjectName())
                            .projectStartedAt(p.getProjectStartedAt())
                            .projectMemberCreatedAt(pm.getProjectMemberCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getByMemberNo(Long memberNo) {
        return portfolioRepository.findFirstByMember_MemberNoAndPortfolioStatusOrderByPortfolioCreatedAtDesc(memberNo, STATUS_ACTIVE)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public PortfolioResponse update(Long portfolioNo, Long memberNo, PortfolioUpdateRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioNo)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
        if (portfolio.getMember() == null || !portfolio.getMember().getMemberNo().equals(memberNo)) {
            throw new IllegalArgumentException("본인의 포트폴리오만 수정할 수 있습니다.");
        }
        portfolio.setPortfolioUserName(emptyToNull(request.getPortfolioUserName()));
        portfolio.setPortfolioUserPhone(emptyToNull(request.getPortfolioUserPhone()));
        portfolio.setPortfolioUserEmail(emptyToNull(request.getPortfolioUserEmail()));
        portfolio.setPortfolioUserCareer(emptyToNull(request.getPortfolioUserCareer()));
        portfolio.setPortfolioUserProject(emptyToNull(request.getPortfolioUserProject()));
        portfolio.setPortfolioUserSkill(emptyToNull(request.getPortfolioUserSkill()));
        portfolio = portfolioRepository.save(portfolio);
        return toResponse(portfolio);
    }

    @Override
    @Transactional
    public void delete(Long portfolioNo, Long memberNo) {
        Portfolio portfolio = portfolioRepository.findById(portfolioNo)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
        if (portfolio.getMember() == null || !portfolio.getMember().getMemberNo().equals(memberNo)) {
            throw new IllegalArgumentException("본인의 포트폴리오만 삭제할 수 있습니다.");
        }
        portfolio.setPortfolioStatus(STATUS_INACTIVE);
        portfolioRepository.save(portfolio);
    }

    @Override
    @Transactional
    public PortfolioResponse syncNotion(Long portfolioNo, Long memberNo) {
        Portfolio portfolio = portfolioRepository.findById(portfolioNo)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
        if (portfolio.getMember() == null || !portfolio.getMember().getMemberNo().equals(memberNo)) {
            throw new IllegalArgumentException("본인의 포트폴리오만 Notion 연동할 수 있습니다.");
        }
        notionPortfolioSyncService.syncPortfolioToNotion(portfolio);
        portfolio = portfolioRepository.save(portfolio);
        return toResponse(portfolio);
    }

    private static String joinList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join("\n", list.stream().filter(s -> s != null && !s.isBlank()).toList());
    }

    private static String emptyToNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    private PortfolioResponse toResponse(Portfolio p) {
        return PortfolioResponse.builder()
                .portfolioNo(p.getPortfolioNo())
                .memberNo(p.getMember() != null ? p.getMember().getMemberNo() : null)
                .portfolioUserName(p.getPortfolioUserName())
                .portfolioUserPhone(p.getPortfolioUserPhone())
                .portfolioUserEmail(p.getPortfolioUserEmail())
                .portfolioUserCareer(p.getPortfolioUserCareer())
                .portfolioUserProject(p.getPortfolioUserProject())
                .portfolioUserSkill(p.getPortfolioUserSkill())
                .portfolioStatus(p.getPortfolioStatus())
                .portfolioCreatedAt(p.getPortfolioCreatedAt())
                .portfolioUpdatedAt(p.getPortfolioUpdatedAt())
                .notionPageId(p.getNotionPageId())
                .notionPageUrl(p.getNotionPageUrl())
                .notionWorkspaceName(p.getNotionWorkspaceName())
                .profileImage(p.getMember() != null ? p.getMember().getMemberProfileImage() : null)
                .build();
    }
}
