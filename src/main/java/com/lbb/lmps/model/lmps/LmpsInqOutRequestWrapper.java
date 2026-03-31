package com.lbb.lmps.model.lmps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nnn.msmart.model.base.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("LmpsInqOutRequestWrapper")
public class LmpsInqOutRequestWrapper extends BaseRequest<LmpsInqOut> {
    // Create a single, static mapper for efficiency.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules() // Ensures Java 8 time types are handled
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    @Override
    public String toString() {
        try {
//            return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(this);
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
