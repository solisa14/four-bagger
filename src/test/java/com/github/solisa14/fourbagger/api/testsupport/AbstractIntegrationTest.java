package com.github.solisa14.fourbagger.api.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  static {
    if (System.getProperty("api.version") == null) {
      System.setProperty("api.version", "1.44");
    }
  }
}
