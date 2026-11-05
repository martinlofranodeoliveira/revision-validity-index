# Security policy

## Scope

Version 0.1.0 is a development version of an in-memory library. It provides no authentication, authorization, durable audit guarantee, encryption or network endpoint. Embedding applications are responsible for access controls and for deciding whether any data is appropriate to store in process memory.

Entity and value limits bound accepted text, not the caller's allocation before a call. Configured structural caps are not an exact JVM heap limit. Very large custom limits can cause process resource exhaustion. State is lost on exit. No automatic pruning or persistence exists.

No telemetry, tracking, remote API access or credential handling is implemented. Fixtures and the demo use synthetic labels only.

## Reporting

If GitHub private vulnerability reporting is enabled for this repository, report privately at:

https://github.com/martinlofranodeoliveira/revision-validity-index/security/advisories/new

If that page is unavailable or private reporting is not enabled, use the general-contact route on the official homepage, https://lofranotecnologia.com.br , to ask for a private reporting channel before sending sensitive details. Do not post exploit details, secrets or personal data publicly. No response-time SLA is promised.

A useful report identifies the affected version, minimal synthetic reproduction, expected versus actual behavior and potential impact. Never include private client code or real personal data. We make no certification or production-readiness claim.

## Português

A versão 0.1.0 é de desenvolvimento e mantém dados apenas em memória. Não oferece autenticação, autorização, criptografia nem auditoria durável. Limites estruturais não são medição exata do heap.

Quando o relato privado do GitHub estiver habilitado, use o endereço `/security/advisories/new` acima. Se não estiver disponível, solicite um canal privado pelo contato geral na página oficial antes de enviar detalhes sensíveis. Não publique segredos, dados pessoais ou detalhes exploráveis em issues. Não há SLA de resposta prometido.
