package com.unifebe.devsecops.config;

/**
 * Credenciais lidas de variaveis de ambiente, nunca gravadas no codigo-fonte.
 *
 * Em producao essas variaveis sao populadas por um cofre de segredos (HashiCorp
 * Vault, AWS Secrets Manager, Azure Key Vault) e injetadas no container pelo
 * orquestrador, o que permite rotacao e revogacao sem novo deploy. O
 * GITHUB_TOKEN do Actions nao serve para isto: e um segredo de CI/build, nao de
 * runtime.
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static String dbPassword() {
        return required("DB_PASSWORD");
    }

    public static String awsAccessKeyId() {
        return required("AWS_ACCESS_KEY_ID");
    }

    public static String awsSecretAccessKey() {
        return required("AWS_SECRET_ACCESS_KEY");
    }

    public static String paymentGatewayApiKey() {
        return required("PAYMENT_GATEWAY_API_KEY");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Variavel de ambiente obrigatoria nao definida: " + name);
        }
        return value;
    }
}
