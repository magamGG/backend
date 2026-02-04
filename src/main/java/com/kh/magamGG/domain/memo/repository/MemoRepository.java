package com.kh.magamGG.domain.memo.repository;

import com.kh.magamGG.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

	/** 회원번호 + 메모 타입으로 목록 조회 (최신순) */
	List<Memo> findByMember_MemberNoAndMemoTypeOrderByMemoCreatedAtDesc(Long memberNo, String memoType);
}
