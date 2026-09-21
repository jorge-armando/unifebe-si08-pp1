# Discussão final: Shift Left e Shift Right na BancoFácil Digital

**Alunos:** Hercilio Zanca Neto e Jorge Armando Bittencourt
**Repositório:** https://github.com/jorge-armando/unifebe-si08-pp1

## Os controles implementados

A pipeline implantada tem cinco gates que bloqueiam o `build-and-push`: detecção de segredos
(Gitleaks), testes unitários (Maven), SAST (Semgrep), SCA (Trivy) e lint de container (Hadolint).
Todos são Shift Left no sentido estrito: rodam antes do merge, sobre código e manifestos, e o
artefato só é publicado no GHCR se todos passarem.

| Controle | Natureza | Extensão Shift Right correspondente |
|---|---|---|
| Testes unitários | Exclusivamente Left | Testes sintéticos e de contrato contra produção |
| SAST (Semgrep) | Exclusivamente Left — exige código-fonte | DAST (OWASP ZAP) em staging; RASP em runtime |
| Lint de Dockerfile (Hadolint) | Exclusivamente Left — analisa a receita, não o container em execução | Políticas de admissão no cluster; detecção de comportamento anômalo (Falco) |
| Detecção de segredos (Gitleaks) | Left, mas incompleto sozinho | Rotação automática via cofre e alerta sobre uso de credencial vazada |
| SCA (Trivy) | Left, mas **insuficiente** sozinho | Rescan contínuo das imagens já publicadas |

## Por que SCA não pode ser apenas Shift Left

O caso mais instrutivo da atividade foi o gate de dependências. Corrigir o `log4j-core` 2.14.1 não
bastou: o Trivy continuou reprovando o `spring-boot-starter-parent` 3.2.5 e as bibliotecas que ele
arrasta. Mesmo após subir para a 3.5.16, o Tomcat 10.1.55 que ela gerencia ainda era vulnerável a
três CVEs CRITICAL de bypass de autenticação, corrigidas só na 10.1.58 — foi preciso sobrescrever
`tomcat.version` para 10.1.60.

A lição é que o conjunto de achados do SCA muda sem que uma linha de código mude. O que hoje passa
verde pode estar vulnerável amanhã, porque a vulnerabilidade foi descoberta depois do deploy. O
Log4Shell é exatamente isso: milhares de aplicações estavam vulneráveis em produção com pipelines
verdes. Um gate que roda só no commit dá uma foto; a segurança de dependências exige um filme.

## O que falta para fechar o ciclo

1. **DAST em staging** — um scan com OWASP ZAP contra o ambiente implantado pegaria falhas que o
   SAST não vê, como erros de configuração, headers ausentes e falhas de autorização.
2. **Rescan contínuo do que já está publicado** — o mesmo Trivy, agendado contra
   `ghcr.io/jorge-armando/unifebe-si08-pp1`, alertando quando uma CVE nova atingir a imagem em uso.
3. **Observabilidade e alertas** — monitorar tentativas de exploração das próprias falhas corrigidas
   (padrões de JNDI para Log4Shell, aspas e `OR '1'='1'` no parâmetro `id` do endpoint `/conta`).
4. **Cofre de segredos de verdade** — a aplicação já lê de variáveis de ambiente, mas ainda falta o
   Vault ou Secrets Manager que as popula, com rotação periódica e auditoria de acesso.
5. **Feature flags e rollout gradual** — liberar correções para uma fração do tráfego permite
   detectar regressão antes de atingir toda a base.
6. **Realimentação do ciclo** — cada incidente de produção deve virar um teste ou uma regra nova de
   SAST, para que a mesma falha seja barrada à esquerda na próxima vez.

Shift Left reduz o custo de corrigir; Shift Right reconhece que nem tudo é previsível antes do
deploy. Para a BancoFácil Digital, que opera sob risco regulatório, o pipeline construído aqui é
condição necessária, mas não suficiente.
