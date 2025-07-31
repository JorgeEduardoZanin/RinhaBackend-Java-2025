package jorge.rinha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = WebServicesAutoConfiguration.class)
@EnableAsync
public class RinhaBackendGraalVmApplication {

	public static void main(String[] args) {
		SpringApplication.run(RinhaBackendGraalVmApplication.class, args);
	}

}
