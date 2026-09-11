package com.hackathonteam7.mainprojectbackend;

import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

/**
 * 기본 application.properties 는 docker-compose 자동 기동을 기대하지만, docker-compose CLI 가
 * 없는 환경(이 프로젝트의 CI/샌드박스)에서는 실패한다. 다른 통합 테스트와 동일하게
 * Testcontainers MySQL 로 컨텍스트를 띄운다.
 */
class MainProjectBackendApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {
    }
}
