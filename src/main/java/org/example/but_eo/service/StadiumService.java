package org.example.but_eo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.StadiumRequest;
import org.example.but_eo.dto.StadiumResponse;
import org.example.but_eo.entity.*;
import org.example.but_eo.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final UsersRepository usersRepository;
    private final FileRepository fileRepository;
    private final StadiumMappingRepository stadiumMappingRepository;

    private Users validateManager(String userId) {
        Users user = usersRepository.findByUserHashId(userId);
        if (user.getDivision() != Users.Division.ADMIN && user.getDivision() != Users.Division.BUSINESS) {
            throw new AccessDeniedException("관리자 또는 사업자만 수행 가능합니다.");
        }
        return user;
    }

    private void validateOwnerOrAdmin(Users user, Stadium stadium) {
        if (user.getDivision() == Users.Division.ADMIN) return;
        if (!stadium.getOwner().getUserHashId().equals(user.getUserHashId())) {
            throw new AccessDeniedException("해당 경기장에 대한 권한이 없습니다.");
        }
    }

    @Transactional
    public void createStadium(StadiumRequest req, String userId) {
        Users user = validateManager(userId);

        Stadium stadium = new Stadium();
        stadium.setStadiumId(UUID.randomUUID().toString());
        stadium.setStadiumName(req.getStadiumName());
        stadium.setStadiumRegion(req.getStadiumRegion());
        stadium.setAvailableDays(req.getAvailableDays());
        stadium.setAvailableHours(req.getAvailableHours());
        stadium.setStadiumCost(req.getStadiumCost());
        stadium.setStadiumMany(req.getStadiumMany());
        stadium.setStadiumTel(req.getStadiumTel());
        stadium.setOwner(user);
        stadiumRepository.save(stadium);

        System.out.println("✅ Stadium 등록 요청 수신");
        System.out.println("✅ 받은 이미지 수: " + (req.getImageFiles() == null ? "null" : req.getImageFiles().size()));
        saveImages(req.getImageFiles(), stadium, user);
    }

    @Transactional
    public void updateStadium(String stadiumId, StadiumRequest req, String userId) {
        Users user = validateManager(userId);
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new IllegalArgumentException("해당 경기장이 존재하지 않습니다."));

        validateOwnerOrAdmin(user, stadium);

        System.out.println("StadiumService.updateStadium 진입");

        // 텍스트 필드 업데이트
        if (isValid(req.getStadiumName())) stadium.setStadiumName(req.getStadiumName());
        if (isValid(req.getStadiumRegion())) stadium.setStadiumRegion(req.getStadiumRegion());
        if (isValid(req.getAvailableDays())) stadium.setAvailableDays(req.getAvailableDays());
        if (isValid(req.getAvailableHours())) stadium.setAvailableHours(req.getAvailableHours());
        if (isValid(req.getStadiumTel())) stadium.setStadiumTel(req.getStadiumTel());

        if (req.getStadiumCost() != null) stadium.setStadiumCost(req.getStadiumCost());
        if (req.getStadiumMany() != null) stadium.setStadiumMany(req.getStadiumMany());

        // 이미지 수정
        List<MultipartFile> newImages = req.getImageFiles();
        System.out.println("이미지 수신: " + (newImages == null ? "null" : newImages.size()));

        if (newImages != null && !newImages.isEmpty()) {
            System.out.println("기존 이미지 삭제 및 새 이미지 저장 시작");

            List<StadiumMapping> oldMappings = stadiumMappingRepository.findAllByStadium_StadiumId(stadiumId);

            for (StadiumMapping mapping : oldMappings) {
                File oldFile = mapping.getFile();

                // 매핑 먼저 제거
                stadium.getStadiumMappingList().remove(mapping);
                stadiumMappingRepository.delete(mapping);

                // 파일 삭제
                if (oldFile != null) {
                    try {
                        String fullPath = System.getProperty("user.dir") + oldFile.getFilePath();
                        Files.deleteIfExists(Paths.get(fullPath));
                        System.out.println("삭제된 이미지: " + fullPath);
                    } catch (IOException e) {
                        System.err.println("이미지 삭제 실패");
                        e.printStackTrace();
                    }
                    fileRepository.deleteById(oldFile.getFileId());
                }
            }

            stadiumMappingRepository.flush();
            stadium.getStadiumMappingList().clear();

            // 새 이미지 저장
            saveImages(newImages, stadium, user);
        } else {
            System.out.println("이미지 변경 없음 (기존 이미지 유지)");
        }

        stadiumRepository.save(stadium);
    }


    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void saveImages(List<MultipartFile> images, Stadium stadium, Users user) {
        if (images == null || images.isEmpty()) {
            System.out.println("저장할 이미지 없음");
            return;
        }

        for (int i = 0; i < Math.min(images.size(), 10); i++) {
            MultipartFile image = images.get(i);
            if (image == null || image.getContentType() == null || !image.getContentType().startsWith("image/")) {
                System.out.println("무시된 이미지: " + (image == null ? "null" : image.getOriginalFilename()));
                continue;
            }

            try {
                String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
                String uploadDir = System.getProperty("user.dir") + "/uploads/stadiums";
                Path path = Paths.get(uploadDir, fileName);

                Files.createDirectories(path.getParent());
                image.transferTo(path.toFile());

                System.out.println("이미지 저장 성공: " + path);

                File file = new File(
                        UUID.randomUUID().toString(),
                        user,
                        fileName,
                        "/uploads/stadiums/" + fileName,
                        LocalDateTime.now()
                );
                fileRepository.save(file);

                StadiumMapping mapping = new StadiumMapping(
                        new StadiumMappingKey(stadium.getStadiumId(), file.getFileId()),
                        stadium,
                        file
                );
                stadiumMappingRepository.save(mapping);
                stadium.getStadiumMappingList().add(mapping);

                System.out.println("✅ 이미지 매핑 저장 완료: " + file.getFilePath());
                System.out.println("✅ stadiumId: " + stadium.getStadiumId());
                System.out.println("✅ mapping 저장 여부: " + stadium.getStadiumMappingList().size());



            } catch (IOException e) {
                System.err.println("이미지 저장 실패");
                e.printStackTrace();
                throw new RuntimeException("이미지 저장 실패", e);
            }
        }
    }

    @Transactional
    public void deleteStadium(String stadiumId, String userId) {
        Users user = validateManager(userId);
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new IllegalArgumentException("해당 경기장이 존재하지 않습니다."));
        validateOwnerOrAdmin(user, stadium);

        List<StadiumMapping> mappings = stadiumMappingRepository.findAllByStadium_StadiumId(stadiumId);
        for (StadiumMapping mapping : mappings) {
            stadiumMappingRepository.delete(mapping);
            fileRepository.deleteById(mapping.getFile().getFileId());
        }

        stadiumRepository.delete(stadium);
    }

    public StadiumResponse getStadiumById(String stadiumId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new NoSuchElementException("경기장 없음"));

        return convertToDto(stadium);
    }

    public List<StadiumResponse> getAll() {
        return stadiumRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private StadiumResponse convertToDto(Stadium stadium) {
        List<StadiumMapping> mappings = stadiumMappingRepository.findAllByStadium_StadiumId(stadium.getStadiumId());

        List<String> urls = mappings.stream()
                .map(mapping -> mapping.getFile().getFilePath())
                .collect(Collectors.toList());

        System.out.println("매핑된 이미지 경로: " + urls);

        return StadiumResponse.builder()
                .stadiumId(stadium.getStadiumId())
                .stadiumName(stadium.getStadiumName())
                .stadiumRegion(stadium.getStadiumRegion())
                .stadiumMany(stadium.getStadiumMany())
                .availableDays(stadium.getAvailableDays())
                .availableHours(stadium.getAvailableHours())
                .stadiumTel(stadium.getStadiumTel())
                .stadiumCost(stadium.getStadiumCost())
                .ownerNickname(stadium.getOwner().getName())
                .imageUrls(urls)
                .build();
    }

}
