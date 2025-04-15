package com.goorm.clonestagram.common.jwt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 ↔ 기기 ID 매핑을 스레드‑안전하게 보관/검증하는 전용 레지스트리.
 *  ‑ 단일 서버에서는 ConcurrentHashMap,
 *  ‑ 다중 서버로 가면 Redis 등으로 내부 구현만 바꿔주면 된다.
 */
@Component
@Slf4j
public class LoginDeviceRegistry {

	private static Map<String, String> store = new ConcurrentHashMap<>();

	/** 이미 다른 기기로 로그인돼 있으면 true */
	public static boolean isDuplicated(String email, String device) {
		log.debug("로그인 기기 확인: " + email + " / " + device);
		String old = store.get(email);
		return old != null && !old.equals(device);
	}

	/** 로그인 성공 시 호출: (email → device) 등록/갱신 */
	public static void save(String email, String device) {
		store.put(email, device);
	}

	/** 요청 시 토큰의 device 값이 현재 저장된 값과 일치하는지 검사 */
	public static boolean match(String email, String device) {
		return device.equals(store.get(email));
	}

	/** 로그아웃 시 호출 */
	public static void remove(String email) {
		store.remove(email);
	}
}
