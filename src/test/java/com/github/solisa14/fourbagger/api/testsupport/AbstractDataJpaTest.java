package com.github.solisa14.fourbagger.api.testsupport;

import com.github.solisa14.fourbagger.api.config.FlywayConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(FlywayConfig.class)
public abstract class AbstractDataJpaTest {

  static {
    if (System.getProperty("api.version") == null) {
      System.setProperty("api.version", "1.44");
    }
  }
}
