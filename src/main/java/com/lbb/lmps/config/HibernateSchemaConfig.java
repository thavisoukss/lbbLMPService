package com.lbb.lmps.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateSchemaConfig implements HibernatePropertiesCustomizer {

    @Value("${app.mbowner-schema}")
    private String mbownerSchema;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.physical_naming_strategy", new LmpsNamingStrategy(mbownerSchema));
    }

    private static class LmpsNamingStrategy implements PhysicalNamingStrategy {

        private final String mbownerSchema;

        LmpsNamingStrategy(String mbownerSchema) {
            this.mbownerSchema = mbownerSchema;
        }

        @Override
        public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            if (logicalName != null && "LMPS_SCHEMA".equals(logicalName.getText())) {
                return Identifier.toIdentifier(mbownerSchema);
            }
            return logicalName;
        }

        @Override
        public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return logicalName;
        }

        @Override
        public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return logicalName;
        }

        @Override
        public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return logicalName;
        }

        @Override
        public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return logicalName;
        }
    }
}