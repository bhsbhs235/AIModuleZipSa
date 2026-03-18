package com.example.zipsa.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Base64;

@Configuration
public class JasyptConfig {

    @Value("${jasypt.encryptor.keystore.password}")
    private String keystorePassword;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor jasyptStringEncryptor() throws Exception {
        // 1. PKCS12 키스토어 로드
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream("zipsa-keystore.p12")) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }

        // 2. AES 비밀키 추출 → Base64 인코딩하여 Jasypt 패스워드로 사용
        SecretKey secretKey = (SecretKey) keyStore.getKey("jasypt-key", keystorePassword.toCharArray());
        String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        // 3. Jasypt 암호화기 설정
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(base64Key);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}
