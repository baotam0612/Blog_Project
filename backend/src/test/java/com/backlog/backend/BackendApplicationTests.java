package com.backlog.backend;

import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SpringBootTest
@Slf4j
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void hash() throws NoSuchAlgorithmException {
		String password = "12345";
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(password.getBytes());

		// 123456
		//E10ADC3949BA59ABBE56E057F20F883E
		// 12345
		//827CCB0EEA8A706C4C34A16891F84E7B
		byte[] digest = md.digest();
		String md5Hash = DatatypeConverter.printHexBinary(digest);
		log.info("md5 round 1: {}", md5Hash);

		md.update(password.getBytes());
		digest = md.digest();
		md5Hash = DatatypeConverter.printHexBinary(digest);
		log.info("MD5 round 2: {}", md5Hash);
	}

}
