package jorge.rinha.config;

import jorge.rinha.dto.response.JsonInstantResponse;

import java.util.List;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(HealthResponseRuntimeHints.class)
public class RuntimeHintsConfig {}

class HealthResponseRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(TypeReference.of(JsonInstantResponse.class), hint -> {
        	hint.withConstructor(
        		    List.of(TypeReference.of(Boolean.class), TypeReference.of(Long.class)),
        		    ExecutableMode.INVOKE
        		);
            hint.withField("json");
            hint.withField("now");
        });
    }
}
