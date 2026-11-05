# Contributing

Revision Validity Index is a development 0.1.0 project by Lofrano Tecnologia, licensed under MIT. Contributions must be original or clearly identified under a compatible license. Do not contribute private client code, secrets, real personal data, fabricated adoption claims or copied proprietary material.

## Local checks

Use Gradle 8.10.2 and JDK 21. Set up the build-local homes described in the README. Reviewed real lock/hash records are required; [docs/integrity.md](docs/integrity.md) describes intentional dependency maintenance without bypasses.

```sh
gradle --dependency-verification strict resolveDependencies testClasses
gradle --offline test
gradle --offline --quiet demoAssert
```

The final command must print exactly:

```text
DEMO_OK old=blue corrected=green removed=missing
```

Keep test and demo gates separate. Do not regenerate integrity records in CI. Do not add publishing, release or remote-deployment workflows.

## Changes worth discussing

Open a repository issue or pull request for a concrete behavior or correction. Explain the effective-time and knowledge-revision consequences of API changes. Add a regression that would fail without the change. Update the English and Portuguese explanations when behavior changes.

For interval logic, keep the independent cell oracle independent: do not replace it with a call to the implementation's rewrite algorithm. Concurrency tests must coordinate with barriers/latches and bounded waits; a sleep is not evidence of a race outcome. Test cap equality, no-ops at full capacity and rejected writes preserving the entire historical answer set.

Preserve deep collection immutability, overflow-safe coordinate comparisons, bounded Unicode inspection and one atomic writer decision. Avoid extra dependencies, automatic pruning and implicit clock/date semantics. No benchmark or compliance claim should be added without a reproducible, narrowly stated basis.

Use four-space Kotlin indentation, UTF-8 and LF. Keep author metadata as the organization name without an invented email address.

## Security and contact

Do not put a potential vulnerability or sensitive reproduction data in a public issue. Follow [SECURITY.md](SECURITY.md). General contact is through https://lofranotecnologia.com.br .

## Português

Contribua apenas com código original ou material de licença compatível, sem dados reais, segredos ou código privado de clientes. Execute testes e demonstração offline separadamente. Mudanças no contrato exigem regressões e atualização das explicações em inglês e português. Preserve o oráculo independente, testes de concorrência sem sleeps como critério, imutabilidade profunda e rejeições atômicas. Relatos sensíveis seguem SECURITY.md.
