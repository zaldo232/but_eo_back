package org.example.but_eo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.TeamJoinRequestDto;
import org.example.but_eo.dto.TeamResponse;
import org.example.but_eo.dto.UpdateTeamRequest;
import org.example.but_eo.entity.*;
import org.example.but_eo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UsersRepository usersRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final ReviewRepository reviewRepository;

    private final Set<Team.Event> soloCompatibleEvents = Set.of(
            Team.Event.BADMINTON,
            Team.Event.TENNIS,
            Team.Event.TABLE_TENNIS,
            Team.Event.BOWLING
    );

    // 팀 생성
    public void createTeam(String teamName, Team.Event event, String region,
                           int memberAge, Team.Team_Case teamCase, String teamDescription,
                           MultipartFile teamImg, String userId) {

        if (teamRepository.existsByTeamName(teamName)) {
            throw new IllegalStateException("이미 존재하는 팀 이름입니다.");
        }

        if (teamMemberRepository.existsByUser_UserHashIdAndTeam_Event(userId, event)) {
            throw new IllegalStateException("이미 해당 종목의 팀에 소속되어 있습니다.");
        }

        Users user = usersRepository.findByUserHashId(userId);
        String teamId = UUID.randomUUID().toString();

        String imgUrl = null;
        if (teamImg != null && !teamImg.isEmpty()) {
            imgUrl = saveImage(teamImg);
        }

        Team team = new Team();
        team.setTeamId(teamId);
        team.setTeamName(teamName);
        team.setEvent(event);
        team.setRegion(region);
        team.setMemberAge(memberAge);
        team.setTeamCase(teamCase);
        team.setTeamDescription(teamDescription);
        team.setTeamImg(imgUrl);
        team.setRating(1000);
        team.setTotalMembers(1);
        team.setMatchCount(0);
        team.setWinCount(0);
        team.setLoseCount(0);
        team.setDrawCount(0);
        team.setTotalReview(0);
        team.setTeamType(Team.Team_Type.TEAM);

        teamRepository.save(team);

        TeamMember teamMember = new TeamMember();
        teamMember.setTeamMemberKey(new TeamMemberKey(userId, teamId));
        teamMember.setUser(user);
        teamMember.setTeam(team);
        teamMember.setType(TeamMember.Type.LEADER);
        teamMember.setPosition("주장");

        teamMemberRepository.save(teamMember);
    }

    // 팀 수정
    @Transactional
    public void updateTeam(String teamId, UpdateTeamRequest req, String userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀이 존재하지 않습니다."));

        TeamMemberKey key = new TeamMemberKey(userId, teamId);
        TeamMember member = teamMemberRepository.findById(key)
                .orElseThrow(() -> new IllegalStateException("팀에 속해있지 않습니다."));

        if (member.getType() != TeamMember.Type.LEADER) {
            throw new IllegalAccessError("팀 수정은 리더만 할 수 있습니다.");
        }

        if (req.getTeamType() != null) {
            Team.Event currentEvent = team.getEvent();
            if (soloCompatibleEvents.contains(currentEvent)) {
                try {
                    Team.Team_Type newType = Team.Team_Type.valueOf(req.getTeamType());
                    Team.Team_Type currentType = team.getTeamType();
                    if (currentType == Team.Team_Type.TEAM && newType == Team.Team_Type.SOLO) {
                        long memberCount = teamMemberRepository.countByTeam_TeamId(teamId);
                        if (memberCount > 1) {
                            throw new IllegalStateException("SOLO로 바꾸려면 혼자여야 합니다.");
                        }
                    }
                    team.setTeamType(newType);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("teamType은 SOLO 또는 TEAM 이어야 합니다.");
                }
            } else {
                throw new IllegalArgumentException("이 종목에서는 팀 타입을 변경할 수 없습니다.");
            }
        }

        if (req.getTeamName() != null) team.setTeamName(req.getTeamName());
        if (req.getRegion() != null) team.setRegion(req.getRegion());
        if (req.getMemberAge() != null) team.setMemberAge(req.getMemberAge());
        if (req.getTeamCase() != null) team.setTeamCase(Team.Team_Case.valueOf(req.getTeamCase()));
        if (req.getTeamDescription() != null) team.setTeamDescription(req.getTeamDescription());

        if (req.getTeamImg() != null && !req.getTeamImg().isEmpty()) {
            String newImgUrl = saveImage(req.getTeamImg());
            team.setTeamImg(newImgUrl);
        }

        teamRepository.save(team);
        teamRepository.flush();
    }

    // 팀 삭제
    @Transactional
    public void deleteTeam(String teamId, String userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀이 존재하지 않습니다."));

        TeamMemberKey key = new TeamMemberKey(userId, teamId);
        TeamMember member = teamMemberRepository.findById(key)
                .orElseThrow(() -> new IllegalStateException("팀에 속해있지 않습니다."));

        if (member.getType() != TeamMember.Type.LEADER) {
            throw new IllegalAccessError("팀 삭제는 리더만 할 수 있습니다.");
        }

        teamInvitationRepository.deleteAllByTeam(team);
        teamMemberRepository.deleteAll(team.getTeamMemberList());

        team.setState(Team.State.DELETED);
        teamRepository.save(team);
    }

    // 팀 목록 조회 (필터 적용)
    public List<TeamResponse> getFilteredTeams(String event, String region, String teamType, String teamCase, String teamName) {
        return teamRepository.findAllByState(Team.State.ACTIVE).stream()
                .filter(team -> event == null || team.getEvent().name().equalsIgnoreCase(event))
                .filter(team -> region == null || team.getRegion().contains(region))
                .filter(team -> teamType == null || team.getTeamType().name().equalsIgnoreCase(teamType))
                .filter(team -> teamCase == null || (team.getTeamCase() != null && team.getTeamCase().name().equalsIgnoreCase(teamCase)))
                .filter(team -> teamName == null || team.getTeamName().contains(teamName))
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    // 팀 단일 조회
    public TeamResponse getTeamDetail(String teamId, String userId) {
        Team team = teamRepository.findWithMembersByTeamIdAndState(teamId, Team.State.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 팀입니다."));
        TeamResponse response = TeamResponse.from(team);

        // 1. 팀 멤버인지
        boolean isMember = teamMemberRepository.existsByUser_UserHashIdAndTeam_TeamId(userId, teamId);
        if (isMember) {
            response.setMyJoinStatus("MEMBER");
        } else if (teamInvitationRepository.existsByUser_UserHashIdAndTeam_TeamIdAndStatusAndDirection(
                userId, teamId, TeamInvitation.Status.PENDING, TeamInvitation.Direction.REQUEST)) {
            response.setMyJoinStatus("PENDING");
        } else {
            response.setMyJoinStatus("NONE");
        }

        // 평균 리뷰 평점 계산 후 세팅
        int totalReview = team.getTotalReview() == 0 ? 0 : team.getTotalReview();
        int reviewCount = reviewRepository.countByTargetTeam_TeamId(teamId); // repository에서 count 쿼리 필요!
        double avg = (reviewCount == 0) ? 0.0 : ((double) totalReview / reviewCount);

        response.setAvgReviewRating(Math.round(avg * 100) / 100.0); // 소수점 둘째자리

        return response;
    }

    // 내 역할 조회 (LEADER / MEMBER / NONE)
    public String getTeamRole(String teamId, String userId) {
        TeamMemberKey key = new TeamMemberKey(userId, teamId);
        return teamMemberRepository.findById(key)
                .map(member -> member.getType().name())
                .orElse("NONE");
    }

    // 리더로 있는 팀 목록 조회
    public List<TeamResponse> getTeamsWhereUserIsLeader(String userId) {
        List<TeamMember> leaderMemberships = teamMemberRepository.findAllByUser_UserHashIdAndType(userId, TeamMember.Type.LEADER);
        return leaderMemberships.stream()
                .map(TeamMember::getTeam)
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    // 내가 속한 팀 목록
    public List<TeamResponse> getTeamsWhereUserIsMember(String userId) {
        List<TeamMember> memberships = teamMemberRepository.findAllByUser_UserHashId(userId);
        return memberships.stream()
                .map(TeamMember::getTeam)
                .filter(team -> team.getState() == Team.State.ACTIVE)
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    // 내부 이미지 저장 헬퍼
    private String saveImage(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/teams/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            return "/uploads/teams/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    //
    public List<TeamJoinRequestDto> getJoinRequests(String teamId, String leaderId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀이 존재하지 않습니다."));

        TeamMemberKey key = new TeamMemberKey(leaderId, teamId);
        TeamMember member = teamMemberRepository.findById(key)
                .orElseThrow(() -> new IllegalStateException("팀에 속해있지 않습니다."));

        if (member.getType() != TeamMember.Type.LEADER) {
            throw new IllegalAccessError("리더만 조회할 수 있습니다.");
        }

        List<TeamInvitation> requests = teamInvitationRepository
                .findAllByTeam_TeamIdAndStatusAndDirection(
                        teamId,
                        TeamInvitation.Status.PENDING,
                        TeamInvitation.Direction.REQUEST
                );

        return requests.stream()
                .map(inv -> TeamJoinRequestDto.builder()
                        .userId(inv.getUser().getUserHashId())
                        .userName(inv.getUser().getName())
                        .profileImg(inv.getUser().getProfile())
                        .requestedAt(inv.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<Team> getMatchedTeams(String teamA, String teamB) {
        return teamMemberRepository.findMatchedTeams(teamA, teamB);
    }

    public String getLeaderHashId(String teamId) {
        Optional<Users> leader = teamMemberRepository.findLeaderByTeamId(teamId);
        if (leader.isPresent()) {
            return leader.get().getUserHashId();
        } else {
            return "NONE";
        }
    }

    public Team getTeam(String teamId) {
        Optional<Team> team = teamRepository.findById(teamId);
        return team.orElse(null);
    }
}
