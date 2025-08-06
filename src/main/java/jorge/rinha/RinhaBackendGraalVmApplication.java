package jorge.rinha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(exclude = WebServicesAutoConfiguration.class)
@EnableScheduling
public class RinhaBackendGraalVmApplication {

	public static void main(String[] args) {
		SpringApplication.run(RinhaBackendGraalVmApplication.class, args);
	}

}
