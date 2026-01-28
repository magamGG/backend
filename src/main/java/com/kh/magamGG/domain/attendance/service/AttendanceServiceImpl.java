package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {
	
	private final AttendanceRepository attendanceRepository;
	
	@Override
	public AttendanceStatisticsResponseDto getAttendanceStatistics(Long memberNo, int year, int month) {
		List<Object[]> results = attendanceRepository.countByMemberNoAndMonth(memberNo, year, month);
		
		List<AttendanceStatisticsResponseDto.TypeCount> typeCounts = results.stream()
			.map(result -> AttendanceStatisticsResponseDto.TypeCount.builder()
				.type((String) result[0])
				.count((Long) result[1])
				.build())
			.collect(Collectors.toList());
		
		Integer totalCount = typeCounts.stream()
			.mapToInt(count -> count.getCount().intValue())
			.sum();
		
		return AttendanceStatisticsResponseDto.builder()
			.typeCounts(typeCounts)
			.totalCount(totalCount)
			.build();
	}
}

