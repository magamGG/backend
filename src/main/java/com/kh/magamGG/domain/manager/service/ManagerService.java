package com.kh.magamGG.domain.manager.service;

import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final ManagerRepository managerRepository;

    public Manager getManager(Long managerNo) {
        return managerRepository.findById(managerNo).orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다."));
    }
}