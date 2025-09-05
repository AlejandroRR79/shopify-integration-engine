import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(scanBasePackages = "com.creditienda")
public class DemoApplication {
    @Value("${config.source:DEFAULT}")
    private String configSource;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @PostConstruct
    public void logConfigSource() {
        System.out.println("🔍 Configuración cargada desde: " + configSource);
    }
}