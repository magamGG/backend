package com.kh.magamGG.domain.memo.service;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.memo.dto.request.MemoCreateRequest;
import com.kh.magamGG.domain.memo.dto.request.MemoUpdateRequest;
import com.kh.magamGG.domain.memo.dto.response.MemoResponse;
import com.kh.magamGG.domain.memo.entity.Memo;
import com.kh.magamGG.domain.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

	private static final String MEMO_TYPE_PERSONAL = "개인";
	private static final String MEMO_TYPE_CALENDAR = "캘린더";
	private static final int MEMO_NAME_MAX_LENGTH = 30;
	private static final int MEMO_TEXT_MAX_LENGTH = 255;

	private final MemoRepository memoRepository;
	private final MemberRepository memberRepository;

	@Override
	@Transactional
	public MemoResponse create(Long memberNo, MemoCreateRequest request) {
		Member member = memberRepository.findById(memberNo)
				.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
		String memoName = truncate(request.getMemoName(), MEMO_NAME_MAX_LENGTH);
		if (memoName == null || memoName.isBlank()) {
			memoName = MEMO_TYPE_CALENDAR.equals(request.getMemoType()) ? "캘린더 메모" : "새 메모";
		}
		String memoText = truncate(request.getMemoText(), MEMO_TEXT_MAX_LENGTH);

		Memo memo = new Memo();
		memo.setMember(member);
		memo.setMemoName(memoName);
		memo.setMemoText(memoText != null ? memoText : "");
		if (MEMO_TYPE_CALENDAR.equals(request.getMemoType())) {
			memo.setMemoType(MEMO_TYPE_CALENDAR);
			if (request.getCalendarMemoDate() != null && !request.getCalendarMemoDate().isBlank()) {
				memo.setCalendarMemoDate(parseCalendarMemoDate(request.getCalendarMemoDate()));
			}
		} else {
			memo.setMemoType(MEMO_TYPE_PERSONAL);
		}
		memo.setMemoCreatedAt(LocalDateTime.now());
		memo.setMemoUpdatedAt(LocalDateTime.now());
		Memo saved = memoRepository.save(memo);
		return MemoResponse.fromEntity(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MemoResponse> listPersonal(Long memberNo) {
		return memoRepository.findByMember_MemberNoAndMemoTypeOrderByMemoCreatedAtDesc(memberNo, MEMO_TYPE_PERSONAL)
				.stream()
				.map(MemoResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<MemoResponse> listCalendar(Long memberNo) {
		return memoRepository.findByMember_MemberNoAndMemoTypeOrderByMemoCreatedAtDesc(memberNo, MEMO_TYPE_CALENDAR)
				.stream()
				.map(MemoResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public MemoResponse update(Long memberNo, Long memoNo, MemoUpdateRequest request) {
		Memo memo = memoRepository.findById(memoNo)
				.orElseThrow(() -> new IllegalArgumentException("메모를 찾을 수 없습니다."));
		if (!memo.getMember().getMemberNo().equals(memberNo)) {
			throw new IllegalArgumentException("본인의 메모만 수정할 수 있습니다.");
		}
		if (!MEMO_TYPE_PERSONAL.equals(memo.getMemoType()) && !MEMO_TYPE_CALENDAR.equals(memo.getMemoType())) {
			throw new IllegalArgumentException("개인/캘린더 메모만 수정할 수 있습니다.");
		}
		String memoName = truncate(request.getMemoName(), MEMO_NAME_MAX_LENGTH);
		if (memoName != null && !memoName.isBlank()) {
			memo.setMemoName(memoName);
		}
		if (request.getMemoText() != null) {
			memo.setMemoText(truncate(request.getMemoText(), MEMO_TEXT_MAX_LENGTH));
		}
		memo.setMemoUpdatedAt(LocalDateTime.now());
		Memo saved = memoRepository.save(memo);
		return MemoResponse.fromEntity(saved);
	}

	@Override
	@Transactional
	public void delete(Long memberNo, Long memoNo) {
		Memo memo = memoRepository.findById(memoNo)
				.orElseThrow(() -> new IllegalArgumentException("메모를 찾을 수 없습니다."));
		if (!memo.getMember().getMemberNo().equals(memberNo)) {
			throw new IllegalArgumentException("본인의 메모만 삭제할 수 있습니다.");
		}
		if (!MEMO_TYPE_PERSONAL.equals(memo.getMemoType()) && !MEMO_TYPE_CALENDAR.equals(memo.getMemoType())) {
			throw new IllegalArgumentException("개인/캘린더 메모만 삭제할 수 있습니다.");
		}
		memoRepository.delete(memo);
	}

	private static String truncate(String value, int maxLength) {
		if (value == null) return null;
		if (value.length() <= maxLength) return value;
		return value.substring(0, maxLength);
	}

	private static LocalDateTime parseCalendarMemoDate(String dateStr) {
		if (dateStr == null || dateStr.isBlank()) return null;
		try {
			LocalDate date = LocalDate.parse(dateStr.trim());
			return date.atTime(LocalTime.MIN);
		} catch (Exception e) {
			return null;
		}
	}
}


