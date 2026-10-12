package com.modernizeai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretRedactorTest {

    @Test
    void masksCommonSecretShapes() {
        assertThat(SecretRedactor.redact("spring.datasource.password=hunter2")).isEqualTo("spring.datasource.password=***REDACTED***");
        assertThat(SecretRedactor.redact("String apiKey = \"abc123\";")).doesNotContain("abc123");
        assertThat(SecretRedactor.redact("token: xyz")).doesNotContain("xyz");
        assertThat(SecretRedactor.redact("key sk-ant-api03-AAAAAAAAAAAAAAAA end")).doesNotContain("sk-ant-api03");
        assertThat(SecretRedactor.redact("AKIAABCDEFGHIJKLMNOP")).isEqualTo(SecretRedactor.MASK);
        assertThat(SecretRedactor.redact("jdbc:mysql://admin:s3cret@db:3306/x")).doesNotContain("s3cret");
    }

    @Test
    void leavesOrdinaryCodeAlone() {
        String code = "public Product save(Product product) { return repo.save(product); }";
        assertThat(SecretRedactor.redact(code)).isEqualTo(code);
    }

    @Test
    void snippetsMarkTheLineAndRedact() {
        String content = "a=1\nb=2\npassword=topsecret\nc=3\n";
        String s = Snippets.around(content, 3);
        assertThat(s).contains("> 3 | password=" + SecretRedactor.MASK).doesNotContain("topsecret");
        assertThat(Snippets.around(content, 99)).isNull();
    }
}
